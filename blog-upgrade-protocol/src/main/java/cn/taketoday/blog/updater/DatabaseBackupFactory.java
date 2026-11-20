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

  public DatabaseBackupFactory(UpgradeFiles files) {
    this(files, DatabaseBackup.CONFIG);
  }

  public DatabaseBackupFactory(UpgradeFiles files, Path config) {
    this.files = files;
    this.config = config;
  }

  public DatabaseBackup configured() throws IOException {
    files.verifyTrusted(config, false);
    Properties values = files.read(config);
    DatabaseBackup backup = new DatabaseBackup(
            Path.of(values.getProperty("dumpExecutable", "/usr/bin/mysqldump")),
            Path.of(values.getProperty("defaultsFile", "/etc/today-blog/backup.cnf")),
            Path.of(values.getProperty("backupRoot", "/var/backups/today-blog")),
            values.getProperty("database"), files);
    files.verifyTrusted(backup.credentialsPath(), true);
    files.verifyTrusted(backup.executablePath().toRealPath(), false);
    files.verifyTrusted(backup.rootPath(), false);
    if (!Files.isExecutable(backup.executablePath())
            || !Files.isRegularFile(backup.credentialsPath(), LinkOption.NOFOLLOW_LINKS)
            || !Files.isDirectory(backup.rootPath(), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("数据库备份环境未就绪");
    }
    return backup;
  }

}
