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

package cn.taketoday.blog.service;

import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeStore;
import infra.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SnapshotOperationServiceTests {

  @TempDir
  Path temp;

  @Test
  void reimportsDiskHistoryAfterEveryDatabaseRollback() throws Exception {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    SnapshotOperationService service = new SnapshotOperationService(jdbc, new UpgradeStore(temp));
    Properties record = new Properties();
    String id = UUID.randomUUID().toString();
    record.setProperty("id", id);
    record.setProperty("status", "READY");
    record.setProperty("createdAt", "2026-11-12T00:00:00Z");
    UpgradeFiles.write(temp.resolve("incoming").resolve(id).resolve("metadata.properties"), record);
    service.upgradeHistory();
    service.upgradeHistory();
    verify(jdbc, times(2)).update(contains("ON DUPLICATE KEY UPDATE"), eq(id), eq("UPGRADE"), eq("READY"),
            isNull(), eq("2026-11-12T00:00:00Z"), isNull(), isNull(), isNull(), anyString());
  }

  @Test
  void preservesUnicodeAndStageMetadata() throws Exception {
    Properties record = new Properties();
    record.setProperty("note", "恢复前保护\n包含=特殊字符");
    record.setProperty("stage.RESTORING", "2026-11-12T00:00:00Z");
    assertThat(SnapshotOperationService.decode(SnapshotOperationService.encode(record))).isEqualTo(record);
  }

  @Test
  void invalidIdentifiersNeverReachDatabase() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    SnapshotOperationService service = new SnapshotOperationService(jdbc, new UpgradeStore(temp));
    assertThatThrownBy(() -> service.upgrade("../outside")).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(jdbc);
  }
}
