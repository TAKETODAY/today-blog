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

package cn.taketoday.blog.snapshot;

import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeStore;
import infra.persistence.EntityManager;
import infra.persistence.PropertyUpdateStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 快照与更新记录索引的写入、覆盖与回放测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
class SnapshotOperationServiceTests {

  @TempDir
  Path temp;

  @Test
  void reimportsDiskHistoryAfterEveryDatabaseRollback() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    SnapshotOperationService service = new SnapshotOperationService(entityManager, new UpgradeStore(temp));
    Properties record = new Properties();
    String id = UUID.randomUUID().toString();
    record.setProperty("id", id);
    record.setProperty("status", AppOperationStatus.READY.name());
    record.setProperty("createdAt", "2026-11-12T00:00:00Z");
    UpgradeFiles.write(temp.resolve("incoming").resolve(id).resolve("metadata.properties"), record);
    service.upgradeHistory();
    service.upgradeHistory();

    ArgumentCaptor<AppOperation> saved = ArgumentCaptor.forClass(AppOperation.class);
    verify(entityManager, times(2)).persist(saved.capture());
    assertThat(saved.getAllValues()).allSatisfy(operation -> {
      assertThat(operation.getId()).isEqualTo(id);
      assertThat(operation.getOperationType()).isEqualTo(AppOperationType.UPGRADE);
      assertThat(operation.getStatus()).isEqualTo(AppOperationStatus.READY);
      assertThat(operation.getCreatedAt()).isEqualTo("2026-11-12T00:00:00Z");
      assertThat(operation.getOperatorId()).isNull();
      assertThat(operation.getFinishedAt()).isNull();
      assertThat(operation.getMetadata()).contains("status=READY");
    });
    verify(entityManager, never()).updateById(any(AppOperation.class), any(PropertyUpdateStrategy.class));
  }

  @Test
  void overwritesExistingIndexRowOnReimport() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    SnapshotOperationService service = new SnapshotOperationService(entityManager, new UpgradeStore(temp));
    Properties record = new Properties();
    String id = UUID.randomUUID().toString();
    record.setProperty("id", id);
    record.setProperty("status", AppOperationStatus.SUCCESS.name());
    record.setProperty("operatorId", "42");
    UpgradeFiles.write(temp.resolve("incoming").resolve(id).resolve("metadata.properties"), record);
    when(entityManager.findById(AppOperation.class, id)).thenReturn(new AppOperation());
    service.upgradeHistory();

    ArgumentCaptor<AppOperation> updated = ArgumentCaptor.forClass(AppOperation.class);
    verify(entityManager).updateById(updated.capture(), any(PropertyUpdateStrategy.class));
    assertThat(updated.getValue().getOperatorId()).isEqualTo(42L);
    verify(entityManager, never()).persist(any(AppOperation.class));
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
    EntityManager entityManager = mock(EntityManager.class);
    SnapshotOperationService service = new SnapshotOperationService(entityManager, new UpgradeStore(temp));
    assertThatThrownBy(() -> service.upgrade("../outside")).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(entityManager);
  }

}
