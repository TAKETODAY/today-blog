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
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作记录在真实数据库中的持久化行为测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@WebAPITest
class AppOperationPersistenceTests {

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  EntityManager entityManager;

  @TempDir
  Path temp;

  @Test
  void snapshotManagementReadsDatabaseAndRejectsUnconnectedExecutor() throws Exception {
    String id = UUID.randomUUID().toString();
    Properties metadata = new Properties();
    metadata.setProperty("scope", "程序、数据库、本地附件、配置");
    metadata.setProperty("password", "must-not-leak");
    jdbc.update("""
            INSERT INTO t_app_snapshot (id, name, status, created_at, protected_snapshot, metadata)
            VALUES (?, ?, 'AVAILABLE', '2026-11-12T00:00:00Z', true, ?)
            """, id, "升级前保护点", SnapshotOperationService.encode(metadata));
    SnapshotService service = new SnapshotService(entityManager);
    assertThat(service.snapshots()).anySatisfy(record -> {
      assertThat(record.id()).isEqualTo(id);
      assertThat(record.protectedSnapshot()).isTrue();
    });
    SnapshotService.SnapshotDetail detail = service.snapshot(id);
    assertThat(detail.scope()).contains("数据库");
    assertThat(detail.snapshot().id()).isEqualTo(id);
    // metadata 中的敏感字段不得进入对外视图。
    assertThat(detail.toString()).doesNotContain("must-not-leak");
    org.assertj.core.api.Assertions.assertThatThrownBy(service::requireExecutor)
            .hasMessageContaining("未执行任何快照操作");
    assertThat(service.capabilities()).containsEntry("create", false).containsEntry("restore", false);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.snapshot(UUID.randomUUID().toString()))
            .hasMessageContaining("不存在");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.snapshot("../outside"))
            .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.snapshot(id).snapshot().protectedSnapshot()).isTrue();
  }

  @Test
  void importsUpdatesAndRebuildsDatabaseIndexWithoutDuplicates() throws Exception {
    String id = UUID.randomUUID().toString();
    Properties metadata = new Properties();
    metadata.setProperty("id", id);
    metadata.setProperty("status", "STARTING");
    metadata.setProperty("operatorId", "42");
    metadata.setProperty("createdAt", "2026-11-12T00:00:00Z");
    metadata.setProperty("note", "中文更新记录");
    UpgradeFiles.write(temp.resolve("incoming").resolve(id).resolve("metadata.properties"), metadata);
    SnapshotOperationService service = new SnapshotOperationService(entityManager, new UpgradeStore(temp));
    assertThat(service.upgradeHistory()).anySatisfy(record -> assertThat(record).isEqualTo(metadata));
    UpgradeFiles.status(temp.resolve("results"), id, "SUCCESS", "更新成功");
    service.upgradeHistory();
    service.upgradeHistory();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_app_operation WHERE id = ?", Long.class, id)).isEqualTo(1L);
    assertThat(service.upgrade(id).getProperty("status")).isEqualTo("SUCCESS");
    // 模拟业务数据库恢复后丢失操作索引，磁盘日志不随业务库回退。
    jdbc.update("DELETE FROM t_app_operation WHERE id = ?", id);
    service.upgradeHistory();
    assertThat(service.upgrade(id).getProperty("note")).isEqualTo("中文更新记录");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_app_operation WHERE id = ?", Long.class, id)).isEqualTo(1L);
  }
}
