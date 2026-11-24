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
import java.nio.file.attribute.GroupPrincipal;
import java.util.Properties;

/**
 * 根据受保护的服务器配置创建数据库备份器。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
public final class DatabaseBackupFactory {

  private final UpgradeFiles files;

  private final Path config;

  private final RuntimeLayout layout;

  public DatabaseBackupFactory(UpgradeFiles files) {
    this(files, DatabaseBackup.CONFIG);
  }

  public DatabaseBackupFactory(UpgradeFiles files, Path config) {
    this(files, config, RuntimeLayout.fromEnvironment());
  }

  public DatabaseBackupFactory(UpgradeFiles files, Path config, RuntimeLayout layout) {
    this.files = files;
    this.config = config;
    this.layout = layout;
  }

  public DatabaseBackup configured() throws IOException {
    Properties values = configuration();
    Path root = backupRoot(values);
    String readGroup = values.getProperty("backupReadGroup", "").strip();
    GroupPrincipal group = readGroup.isEmpty() ? null
            : root.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(readGroup);
    DatabaseBackup backup = new DatabaseBackup(
            Path.of(values.getProperty("dumpExecutable", "/usr/bin/mysqldump")),
            Path.of(values.getProperty("defaultsFile", "/etc/today-blog/backup.cnf")),
            root, values.getProperty("database"), files, group);
    files.verifyTrusted(backup.credentialsPath(), true);
    files.verifyTrusted(backup.executablePath().toRealPath(), false);
    if (!Files.isExecutable(backup.executablePath())
            || !Files.isRegularFile(backup.credentialsPath(), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("数据库备份环境未就绪");
    }
    return backup;
  }

  public Path archive(String location) throws IOException {
    Path relative = StoredPath.parse(location);
    if (relative.getNameCount() != 2 || !relative.getFileName().toString().equals(DatabaseBackup.ARCHIVE_NAME)) {
      throw new IOException("不是受管理的数据库备份文件");
    }
    files.checkId(relative.getName(0).toString());
    Path archive = backupRoot(configuration()).resolve(relative);
    files.verifyTrusted(archive, false);
    if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(archive)) {
      throw new IOException("数据库备份文件不可读取");
    }
    return archive;
  }

  private Properties configuration() throws IOException {
    files.verifyTrusted(config, false);
    return files.read(config);
  }

  private Path backupRoot(Properties values) throws IOException {
    Path root = Path.of(values.getProperty("backupRoot", layout.backups().toString()));
    files.verifyTrusted(root, false);
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("数据库备份目录未就绪");
    }
    return root;
  }

}
