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

  private final UpgradeFiles files;

  public DatabaseBackup(Path executable, Path credentials, Path root, String database) {
    this(executable, credentials, root, database, new UpgradeFiles());
  }

  public DatabaseBackup(Path executable, Path credentials, Path root, String database,
          UpgradeFiles files) {
    if (!executable.isAbsolute() || !credentials.isAbsolute() || !root.isAbsolute()
            || database == null || !database.matches("[A-Za-z0-9_]+")) {
      throw new IllegalArgumentException("数据库备份服务器配置无效");
    }
    this.executable = executable;
    this.credentials = credentials;
    this.root = root;
    this.database = database;
    this.files = files;
  }

  public long usableSpace() throws IOException {
    return Files.getFileStore(root).getUsableSpace();
  }

  Path executablePath() {
    return executable;
  }

  Path credentialsPath() {
    return credentials;
  }

  Path rootPath() {
    return root;
  }

  public DatabaseBackupResult create(String id, int timeoutSeconds) throws Exception {
    files.checkId(id);
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
      complete = true;
      return new DatabaseBackupResult(id, archive, Files.size(archive),
              java.util.HexFormat.of().formatHex(digest.digest()), Instant.now());
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
