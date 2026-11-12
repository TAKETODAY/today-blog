/*
 * Copyright 2017 - 2026 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.updater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * 仅从服务器受保护配置构造命令。导出错误输出不进入日志或 Web 响应。
 */
public final class DatabaseBackup {

  public static final Path CONFIG = Path.of("/etc/today-blog/upgrade.properties");

  private final Path executable;

  private final Path credentials;

  private final Path root;

  private final String database;

  public DatabaseBackup(Path executable, Path credentials, Path root, String database) {
    if (!executable.isAbsolute() || !credentials.isAbsolute() || !root.isAbsolute()
            || database == null || !database.matches("[A-Za-z0-9_]+")) {
      throw new IllegalArgumentException("数据库备份服务器配置无效");
    }
    this.executable = executable;
    this.credentials = credentials;
    this.root = root;
    this.database = database;
  }

  public static DatabaseBackup configured() throws IOException {
    trusted(CONFIG, false);
    Properties config = UpgradeFiles.read(CONFIG);
    DatabaseBackup backup = new DatabaseBackup(Path.of(config.getProperty("dumpExecutable", "/usr/bin/mysqldump")),
            Path.of(config.getProperty("defaultsFile", "/etc/today-blog/backup.cnf")),
            Path.of(config.getProperty("backupRoot", "/var/backups/today-blog")), config.getProperty("database"));
    trusted(backup.credentials, true);
    trusted(backup.executable.toRealPath(), false);
    trusted(backup.root, false);
    if (!Files.isExecutable(backup.executable) || !Files.isRegularFile(backup.credentials, LinkOption.NOFOLLOW_LINKS)
            || !Files.isDirectory(backup.root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("数据库备份环境未就绪");
    }
    return backup;
  }

  public long usableSpace() throws IOException {
    return Files.getFileStore(root).getUsableSpace();
  }

  private static void trusted(Path path, boolean secret) throws IOException {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)
            || !Files.getOwner(path).getName().equals("root")) {
      throw new IOException("备份配置、工具及目录必须由 root 管理");
    }
    var permissions = Files.getPosixFilePermissions(path);
    var forbidden = PosixFilePermissions.fromString(secret ? "---rwxrwx" : "----w--w-");
    if (permissions.stream().anyMatch(forbidden::contains)) {
      throw new IOException("备份配置、工具或目录权限不安全");
    }
    Path parent = path.getParent();
    while (parent != null) {
      if (!Files.getOwner(parent).getName().equals("root")
              || Files.getPosixFilePermissions(parent).stream().anyMatch(
                      PosixFilePermissions.fromString("----w--w-")::contains)) {
        throw new IOException("备份路径的父目录必须由 root 管理且不可由其他用户写入");
      }
      parent = parent.getParent();
    }
  }

  public Properties create(String id, int timeoutSeconds) throws Exception {
    UpgradeFiles.checkId(id);
    if (timeoutSeconds <= 0 || timeoutSeconds > 3600) {
      throw new IllegalArgumentException("备份超时无效");
    }
    if (!Files.isExecutable(executable) || !Files.isRegularFile(credentials, LinkOption.NOFOLLOW_LINKS)
            || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("数据库备份工具、凭据或目录未就绪");
    }
    if (Files.getFileStore(root).getUsableSpace() < 256L * 1024 * 1024) {
      throw new IOException("备份磁盘可用空间不足 256 MiB");
    }
    Path task = Files.createDirectory(root.resolve(id), PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rwx------")));
    Path raw = Files.createFile(task.resolve("database.sql.tmp"), PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rw-------")));
    Path compressed = task.resolve("database.sql.gz.tmp");
    Path archive = task.resolve("database.sql.gz");
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    Process process = null;
    boolean complete = false;
    try {
      process = new ProcessBuilder(executable.toString(), "--defaults-file=" + credentials,
              "--single-transaction", "--quick", "--skip-lock-tables", "--hex-blob", "--routines", "--events",
              "--triggers", "--databases", database).redirectOutput(raw.toFile())
              .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        throw new IOException("数据库备份超时");
      }
      if (process.exitValue() != 0 || Files.size(raw) == 0) {
        throw new IOException("数据库导出失败，请检查服务器备份配置与数据库权限");
      }
      Files.createFile(compressed, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      try (var input = Files.newInputStream(raw);
              var output = new GZIPOutputStream(new java.security.DigestOutputStream(Files.newOutputStream(compressed), digest))) {
        byte[] buffer = new byte[65536];
        int count;
        while ((count = input.read(buffer)) != -1) {
          if (System.nanoTime() >= deadline) {
            throw new IOException("数据库备份压缩超时");
          }
          output.write(buffer, 0, count);
        }
      }
      Files.move(compressed, archive, StandardCopyOption.ATOMIC_MOVE);
      Properties result = new Properties();
      result.setProperty("backupId", id);
      result.setProperty("backupStatus", "SUCCESS");
      result.setProperty("backupBytes", Long.toString(Files.size(archive)));
      result.setProperty("backupSha256", java.util.HexFormat.of().formatHex(digest.digest()));
      result.setProperty("backupCompletedAt", Instant.now().toString());
      // 备份目录私有，即使元信息可读也不会暴露 SQL 数据。
      UpgradeFiles.write(task.resolve("backup.properties"), result);
      complete = true;
      return result;
    }
    finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
        process.waitFor();
      }
      Files.deleteIfExists(raw);
      Files.deleteIfExists(compressed);
      if (!complete) {
        Files.deleteIfExists(archive);
      }
    }
  }
}
