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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeSettingsTests {

  @TempDir
  Path temp;

  @Test
  void validatesOptionsAndPersistsDefaults() throws Exception {
    UpgradeStore store = new UpgradeStore(temp);
    assertThat(store.settings().backupEnabled()).isTrue();
    UpgradeOptions options = new UpgradeOptions(false, 30, 10, "测试");
    store.saveSettings(options);
    assertThat(new UpgradeStore(temp).settings()).isEqualTo(options);
    assertThatThrownBy(() -> new UpgradeOptions(true, 29, 90, "")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new UpgradeOptions(true, 600, 601, "")).isInstanceOf(IllegalArgumentException.class);
    Properties bad = new Properties();
    bad.setProperty("backupEnabled", "yes");
    assertThatThrownBy(() -> UpgradeOptions.from(bad)).isInstanceOf(IllegalArgumentException.class);
    bad.setProperty("backupEnabled", "true");
    bad.setProperty("healthTimeoutSeconds", "1.2");
    assertThatThrownBy(() -> UpgradeOptions.from(bad)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void snapshotAndHistorySurviveSettingsChanges() throws Exception {
    String id = UUID.randomUUID().toString();
    Path metadata = temp.resolve("incoming").resolve(id).resolve("metadata.properties");
    Properties values = new Properties();
    values.setProperty("status", "READY");
    values.setProperty("id", id);
    UpgradeFiles.write(metadata, values);
    UpgradeStore store = new UpgradeStore(temp);
    store.snapshot(id, UpgradeOptions.defaults(), 42, values);
    store.saveSettings(new UpgradeOptions(false, 30, 10, "changed"));
    assertThat(UpgradeOptions.from(UpgradeFiles.read(metadata.getParent().resolve("options.properties"))))
            .isEqualTo(UpgradeOptions.defaults());
    UpgradeFiles.status(temp.resolve("results"), id, "BACKING_UP", "备份中");
    UpgradeFiles.status(temp.resolve("results"), id, "FAILED", "失败");
    Properties record = new UpgradeStore(temp).history().get(0);
    assertThat(record.getProperty("operatorId")).isEqualTo("42");
    assertThat(record.getProperty("status")).isEqualTo("FAILED");
    assertThat(record.getProperty("stage.BACKING_UP")).isNotBlank();
    assertThat(record.getProperty("finishedAt")).isNotBlank();
    assertThatThrownBy(() -> store.snapshot(id, UpgradeOptions.defaults(), 42, values))
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void dumpIsCompressedAndPrivate() throws Exception {
    Path executable = script("printf 'CREATE TABLE example (id int);'");
    Path credentials = Files.createFile(temp.resolve("backup.cnf"));
    String id = UUID.randomUUID().toString();
    DatabaseBackup backup = new DatabaseBackup(executable, credentials, temp, "today");
    Properties result = backup.create(id, 30);
    Path archive = temp.resolve(id).resolve("database.sql.gz");
    assertThat(result.getProperty("backupStatus")).isEqualTo("SUCCESS");
    try (var input = new GZIPInputStream(Files.newInputStream(archive))) {
      assertThat(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("CREATE TABLE");
    }
    assertThat(Files.getPosixFilePermissions(archive))
            .isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    assertThat(Files.exists(archive.getParent().resolve("database.sql.tmp"))).isFalse();
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
