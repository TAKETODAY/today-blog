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

import java.nio.file.Path;
import java.nio.file.Files;
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
 * 备份与更新记录索引的写入、覆盖与回放测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
class BackupOperationServiceTests {

  @TempDir
  Path temp;

  @Test
  void reimportsDiskHistoryAfterEveryDatabaseRollback() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupOperationService service = new BackupOperationService(entityManager, new UpgradeStore(temp));
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
    BackupOperationService service = new BackupOperationService(entityManager, new UpgradeStore(temp));
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
    assertThat(BackupOperationService.decode(BackupOperationService.encode(record))).isEqualTo(record);
  }

  @Test
  void replaysSnapshotsAndMaintenanceOperationsAfterDatabaseRollback() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupOperationService operations = new BackupOperationService(entityManager, new UpgradeStore(temp));
    BackupIndexService index = new BackupIndexService(entityManager, operations, temp);
    Properties snapshot = snapshotRecord();
    String id = snapshot.getProperty("id");
    UpgradeFiles.write(temp.resolve("snapshots").resolve(id + ".properties"), snapshot);
    Properties operation = new Properties();
    String operationId = UUID.randomUUID().toString();
    operation.setProperty("id", operationId);
    operation.setProperty("operationType", "SNAPSHOT_RESTORE");
    operation.setProperty("status", "MAINTENANCE_REQUIRED");
    operation.setProperty("snapshotId", id);
    operation.setProperty("createdAt", "2026-11-13T00:00:00Z");
    UpgradeFiles.write(temp.resolve("snapshot-operations").resolve(operationId + ".properties"), operation);

    index.synchronize();
    index.synchronize();

    ArgumentCaptor<AppSnapshot> snapshots = ArgumentCaptor.forClass(AppSnapshot.class);
    verify(entityManager, times(2)).persist(snapshots.capture());
    assertThat(snapshots.getAllValues()).allSatisfy(saved -> {
      assertThat(saved.getId()).isEqualTo(id);
      assertThat(saved.isProtectedSnapshot()).isTrue();
      assertThat(saved.getStatus()).isEqualTo(AppSnapshotStatus.AVAILABLE);
      assertThat(saved.getTotalBytes()).isEqualTo(123L);
    });
    ArgumentCaptor<AppOperation> history = ArgumentCaptor.forClass(AppOperation.class);
    verify(entityManager, times(2)).persist(history.capture());
    assertThat(history.getAllValues()).allSatisfy(saved -> {
      assertThat(saved.getStatus()).isEqualTo(AppOperationStatus.MAINTENANCE_REQUIRED);
      assertThat(saved.getSnapshotId()).isEqualTo(id);
    });
  }

  @Test
  void replayOverwritesProtectionAndRetainsDeletionTombstone() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupIndexService index = new BackupIndexService(entityManager,
            new BackupOperationService(entityManager, new UpgradeStore(temp)), temp);
    Properties record = snapshotRecord();
    String id = record.getProperty("id");
    record.setProperty("status", "DELETED");
    record.setProperty("protectedSnapshot", "false");
    record.remove("manifestSha256");
    UpgradeFiles.write(temp.resolve("snapshots").resolve(id + ".properties"), record);
    when(entityManager.findById(AppSnapshot.class, id)).thenReturn(new AppSnapshot());

    index.synchronize();

    ArgumentCaptor<AppSnapshot> updated = ArgumentCaptor.forClass(AppSnapshot.class);
    verify(entityManager).updateById(updated.capture(), any(PropertyUpdateStrategy.class));
    assertThat(updated.getValue().getStatus()).isEqualTo(AppSnapshotStatus.DELETED);
    assertThat(updated.getValue().isProtectedSnapshot()).isFalse();
    assertThat(updated.getValue().getManifestSha256()).isNull();
    verify(entityManager, never()).persist(any(AppSnapshot.class));
  }

  @Test
  void rejectsCorruptSnapshotBeforeDatabaseAccess() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupIndexService index = new BackupIndexService(entityManager,
            new BackupOperationService(entityManager, new UpgradeStore(temp)), temp);
    Properties record = snapshotRecord();
    record.setProperty("protectedSnapshot", "yes");
    UpgradeFiles.write(temp.resolve("snapshots").resolve(record.getProperty("id") + ".properties"), record);

    assertThatThrownBy(index::synchronize).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(entityManager);
  }

  @Test
  void rejectsMismatchedDiskIdentityBeforeDatabaseAccess() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupIndexService index = new BackupIndexService(entityManager,
            new BackupOperationService(entityManager, new UpgradeStore(temp)), temp);
    UpgradeFiles.write(temp.resolve("snapshots").resolve(UUID.randomUUID() + ".properties"), snapshotRecord());

    assertThatThrownBy(index::synchronize).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(entityManager);
  }

  private static Properties snapshotRecord() {
    Properties record = new Properties();
    record.setProperty("id", UUID.randomUUID().toString());
    record.setProperty("name", "恢复前保护");
    record.setProperty("status", "AVAILABLE");
    record.setProperty("version", "4.0");
    record.setProperty("build", "42");
    record.setProperty("createdAt", "2026-11-13T00:00:00Z");
    record.setProperty("protectedSnapshot", "true");
    record.setProperty("totalBytes", "123");
    record.setProperty("manifestSha256", "a".repeat(64));
    return record;
  }

  @Test
  void availableSnapshotRequiresManifestDigest() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupIndexService index = new BackupIndexService(entityManager,
            new BackupOperationService(entityManager, new UpgradeStore(temp)), temp);
    Properties record = snapshotRecord();
    record.remove("manifestSha256");
    UpgradeFiles.write(temp.resolve("snapshots").resolve(record.getProperty("id") + ".properties"), record);

    assertThatThrownBy(index::synchronize).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(entityManager);
  }

  @Test
  void refusesSymbolicLinkRecords() throws Exception {
    EntityManager entityManager = mock(EntityManager.class);
    BackupIndexService index = new BackupIndexService(entityManager,
            new BackupOperationService(entityManager, new UpgradeStore(temp)), temp);
    Properties record = snapshotRecord();
    Path outside = temp.resolve("outside.properties");
    UpgradeFiles.write(outside, record);
    Files.createDirectory(temp.resolve("snapshots"));
    Files.createSymbolicLink(temp.resolve("snapshots").resolve(record.getProperty("id") + ".properties"), outside);

    assertThatThrownBy(index::synchronize).isInstanceOf(java.io.IOException.class);
    verifyNoInteractions(entityManager);
  }

  @Test
  void invalidIdentifiersNeverReachDatabase() {
    EntityManager entityManager = mock(EntityManager.class);
    BackupOperationService service = new BackupOperationService(entityManager, new UpgradeStore(temp));
    assertThatThrownBy(() -> service.upgrade("../outside")).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(entityManager);
  }

}
