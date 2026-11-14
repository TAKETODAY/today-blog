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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeSettingsTests {

  @TempDir
  Path temp;

  @Test
  void validatesOptions() {
    assertThat(new UpgradeOptions(true, 30, 10, "测试").backupTimeoutSeconds()).isEqualTo(30);
    assertThatThrownBy(() -> new UpgradeOptions(true, 29, 90, ""))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new UpgradeOptions(true, 600, 601, ""))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new UpgradeOptions(true, 600, 90, "a".repeat(501)))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void dumpIsCompressedAndPrivate() throws Exception {
    Path executable = script("printf 'CREATE TABLE example (id int);'");
    Path credentials = Files.createFile(temp.resolve("backup.cnf"));
    String id = UUID.randomUUID().toString();
    DatabaseBackup backup = new DatabaseBackup(executable, credentials, temp, "today");
    DatabaseBackupResult result = backup.create(id, 30);
    Path archive = temp.resolve(id).resolve("database.sql.gz");
    assertThat(result.location()).isEqualTo(archive);
    assertThat(result.bytes()).isPositive();
    try (var input = new GZIPInputStream(Files.newInputStream(archive))) {
      assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("CREATE TABLE");
    }
    assertThat(Files.getPosixFilePermissions(archive))
            .isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    assertThat(Files.exists(archive.getParent().resolve("database.sql.tmp"))).isFalse();
    assertThat(Files.exists(archive.getParent().resolve("backup.properties"))).isFalse();
  }

  @Test
  void failedDumpIsNotReportedAsBackup() throws Exception {
    Path executable = script("printf 'partial'; printf 'password=secret' >&2; exit 1");
    Path credentials = Files.createFile(temp.resolve("backup.cnf"));
    String id = UUID.randomUUID().toString();
    assertThatThrownBy(() -> new DatabaseBackup(executable, credentials, temp, "today").create(id, 30))
            .hasMessageContaining("导出失败").hasMessageNotContaining("secret");
    assertThat(Files.exists(temp.resolve(id).resolve("database.sql.gz"))).isFalse();
    assertThat(Files.exists(temp.resolve(id).resolve("database.sql.tmp"))).isFalse();
  }

  @Test
  void timedOutDumpIsKilledWithoutPublishingArchive() throws Exception {
    Path executable = script("exec sleep 10");
    Path credentials = Files.createFile(temp.resolve("backup.cnf"));
    String id = UUID.randomUUID().toString();
    assertThatThrownBy(() -> new DatabaseBackup(executable, credentials, temp, "today").create(id, 1))
            .hasMessageContaining("超时");
    assertThat(Files.exists(temp.resolve(id).resolve("database.sql.gz"))).isFalse();
  }

  private Path script(String body) throws Exception {
    Path path = temp.resolve("dump");
    Files.writeString(path, "#!/bin/sh\n" + body + "\n");
    Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
    return path;
  }

}
