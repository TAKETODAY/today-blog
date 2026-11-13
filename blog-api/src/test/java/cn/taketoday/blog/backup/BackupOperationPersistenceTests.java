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

package cn.taketoday.blog.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeStore;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.persistence.EntityManager;
import infra.persistence.annotation.EntityRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 操作记录在真实数据库中的持久化行为测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@WebAPITest
class BackupOperationPersistenceTests {

  @Autowired
  EntityManager entityManager;

  @TempDir
  Path temp;

  @Test
  void backupManagementReadsDatabaseAndRejectsUnconnectedExecutor() throws Exception {
    String id = UUID.randomUUID().toString();
    Properties metadata = new Properties();
    metadata.setProperty("scope", "程序、数据库、本地附件、配置");
    metadata.setProperty("password", "must-not-leak");
    Backup backup = new Backup();
    backup.setId(id);
    backup.setName("升级前保护点");
    backup.setStatus(BackupStatus.AVAILABLE);
    backup.setCreatedAt("2026-11-12T00:00:00Z");
    backup.setProtectedBackup(true);
    backup.setMetadata(BackupOperationService.encode(metadata));
    entityManager.persist(backup);
    BackupService service = new BackupService(entityManager);
    assertThat(service.backups()).anySatisfy(record -> {
      assertThat(record.id()).isEqualTo(id);
      assertThat(record.protectedBackup()).isTrue();
    });
    BackupService.BackupDetail detail = service.backup(id);
    assertThat(detail.scope()).contains("数据库");
    assertThat(detail.backup().id()).isEqualTo(id);
    // metadata 中的敏感字段不得进入对外视图。
    assertThat(detail.toString()).doesNotContain("must-not-leak");
    assertThatThrownBy(service::requireExecutor).hasMessageContaining("未执行任何备份操作");
    assertThat(service.capabilities()).containsEntry("create", false).containsEntry("restore", false);
    assertThatThrownBy(() -> service.backup(UUID.randomUUID().toString())).hasMessageContaining("不存在");
    assertThatThrownBy(() -> service.backup("../outside")).isInstanceOf(IllegalArgumentException.class);
    assertThat(service.backup(id).backup().protectedBackup()).isTrue();
  }

  @Test
  void importsUpdatesAndRebuildsDatabaseIndexWithoutDuplicates() throws Exception {
    String id = UUID.randomUUID().toString();
    Properties metadata = new Properties();
    metadata.setProperty("id", id);
    metadata.setProperty("status", BackupOperationStatus.STARTING.name());
    metadata.setProperty("operatorId", "42");
    metadata.setProperty("createdAt", "2026-11-12T00:00:00Z");
    metadata.setProperty("note", "中文更新记录");
    UpgradeFiles.write(temp.resolve("incoming").resolve(id).resolve("metadata.properties"), metadata);
    BackupOperationService service = new BackupOperationService(entityManager, new UpgradeStore(temp));
    assertThat(service.upgradeHistory()).anySatisfy(record -> assertThat(record).isEqualTo(metadata));
    UpgradeFiles.status(temp.resolve("results"), id, BackupOperationStatus.SUCCESS.name(), "更新成功");
    service.upgradeHistory();
    service.upgradeHistory();
    assertThat(entityManager.count(BackupOperation.class, new BackupOperationId(id))).isEqualTo(1L);
    assertThat(service.upgrade(id).getProperty("status")).isEqualTo(BackupOperationStatus.SUCCESS.name());
    // 模拟业务数据库恢复后丢失操作索引，磁盘日志不随业务库回退。
    entityManager.delete(BackupOperation.class, id);
    service.upgradeHistory();
    assertThat(service.upgrade(id).getProperty("note")).isEqualTo("中文更新记录");
    assertThat(entityManager.count(BackupOperation.class, new BackupOperationId(id))).isEqualTo(1L);
  }

  /**
   * 按主键统计操作记录数量。
   */
  @EntityRef(BackupOperation.class)
  static class BackupOperationId {

    public final String id;

    BackupOperationId(String id) {
      this.id = id;
    }
  }
}
