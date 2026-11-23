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

package cn.taketoday.blog.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeFiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证数据导出的目录边界。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/23
 */
class DataCommandsTests {

  @TempDir
  Path temp;

  @Test
  void exportsManagedDataAndSkipsTemporaryAndExternalFiles() throws Exception {
    var layout = new RuntimeLayout(temp.resolve("custom-runtime"));
    layout.ensureDirectories();
    Files.writeString(Files.createDirectories(layout.data().resolve("upload")).resolve("photo.png"), "photo");
    Files.writeString(Files.createDirectories(layout.backups().resolve("1")).resolve("database.sql.gz"), "backup");
    Files.writeString(Files.createDirectories(layout.packages().resolve("1")).resolve("package.zip"), "package");
    Files.writeString(Files.createDirectories(layout.multipart()).resolve("request.tmp"), "temporary");
    Files.writeString(Files.createDirectories(temp.resolve("external")).resolve("legacy.png"), "external");
    Files.writeString(layout.logs().resolve("blog-api.log"), "log");

    var files = new UpgradeFiles();
    var backups = new DatabaseBackupFactory(files, temp.resolve("upgrade.properties"), layout);
    var commands = new DataCommands(layout, files, backups, temp.resolve("data.properties"));
    Path output = temp.resolve("export.zip");
    commands.run(new String[] { "export", "--output", output.toString() });

    try (var archive = new ZipFile(output.toFile())) {
      assertThat(archive.stream().filter(entry -> !entry.isDirectory()).map(entry -> entry.getName()))
              .containsExactlyInAnyOrder("today-blog-data/data/upload/photo.png",
                      "today-blog-data/backups/db/1/database.sql.gz", "today-blog-data/upgrade/packages/1/package.zip",
                      "today-blog-data/META-INF/export.properties");
    }
  }

}
