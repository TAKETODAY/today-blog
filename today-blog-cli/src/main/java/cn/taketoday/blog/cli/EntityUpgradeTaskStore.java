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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.blog.updater.DatabaseBackupResult;
import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.updater.StoredPath;
import infra.persistence.EntityManager;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.Id;
import infra.persistence.annotation.Table;
import infra.persistence.annotation.UpdateBy;
import infra.transaction.support.TransactionOperations;

/**
 * 使用 CLI 自己的实体映射读写升级任务，不依赖 API 模块。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
final class EntityUpgradeTaskStore implements UpgradeTaskStore {

  private final EntityManager entities;

  private final TransactionOperations transactions;

  EntityUpgradeTaskStore(EntityManager entities, TransactionOperations transactions) {
    this.entities = entities;
    this.transactions = transactions;
  }

  @Override
  public boolean begin(long id) {
    Claim claim = new Claim();
    claim.recordId = id;
    claim.expectedStatus = "STARTING";
    claim.status = "VALIDATING";
    claim.message = "校验升级包";
    return entities.update(claim) == 1;
  }

  @Override
  public UpgradeTask load(long id) {
    TaskRow task = requireTask(id);
    List<ReleaseMapping> mappings = entities.find(MappingRow.class, Map.of("recordId", id)).stream()
            .sorted(Comparator.comparing(mapping -> mapping.name))
            .map(mapping -> ReleaseMapping.fromStorage(mapping.name, ReleaseMapping.Mode.valueOf(mapping.mode),
                    mapping.source, mapping.target))
            .toList();
    return new UpgradeTask(id, task.packageId, task.packagePath, task.packageVersion,
            task.packageBuild, task.oldBuild, task.backupEnabled, task.backupTimeoutSeconds,
            task.healthTimeoutSeconds, task.deletePackageAfterUpgrade, mappings);
  }

  private TaskRow requireTask(long id) {
    TaskRow task = entities.findById(TaskRow.class, id);
    if (task == null) {
      throw new IllegalStateException("升级任务不存在: " + id);
    }
    return task;
  }

  @Override
  public void status(long id, String status, String message) {
    TaskRow task = new TaskRow();
    task.id = id;
    task.status = status;
    task.message = message;
    update(task, Set.of("status", "message"));
  }

  @Override
  public void started(long id, String releasePath) {
    StoredPath.parse(releasePath);
    TaskRow task = new TaskRow();
    task.id = id;
    task.startedAt = Instant.now();
    task.releasePath = releasePath;
    update(task, Set.of("startedAt", "releasePath"));
  }

  @Override
  public void backup(long id, DatabaseBackupResult backup) {
    StoredPath.parse(backup.relativeLocation());
    transactions.executeWithoutResult(transaction -> {
      TaskRow task = requireTask(id);
      BackupRow row = new BackupRow();
      row.name = "升级前数据库备份 " + task.packageVersion;
      row.status = "AVAILABLE";
      row.version = task.packageVersion;
      row.build = Long.toString(task.packageBuild);
      row.createdAt = backup.completedAt();
      row.manifestSha256 = backup.sha256();
      row.totalBytes = backup.bytes();
      row.sourceOperationId = id;
      row.location = backup.relativeLocation();
      row.scope = "数据库";
      entities.persist(row);

      task.backupPath = row.location;
      task.backupBytes = row.totalBytes;
      task.backupSha256 = row.manifestSha256;
      task.backupCompletedAt = row.createdAt;
      if (update(task, Set.of("backupPath", "backupBytes", "backupSha256", "backupCompletedAt")) != 1) {
        throw new IllegalStateException("无法保存升级任务的备份信息: " + id);
      }
    });
  }

  @Override
  public void finished(long id, String status, String message) {
    TaskRow task = new TaskRow();
    task.id = id;
    task.status = status;
    task.message = message;
    task.finishedAt = Instant.now();
    update(task, Set.of("status", "message", "finishedAt"));
  }

  private int update(TaskRow task, Set<String> fields) {
    return entities.updateById(task, (entity, property) -> fields.contains(property.getName()));
  }

  @Override
  public void deletePackage(long packageId) {
    entities.delete(PackageRow.class, packageId);
  }

  @Table("t_upgrade_record")
  static class TaskRow {
    @Id
    public Long id;
    public long packageId;
    public String packagePath;
    public String packageVersion;
    public long packageBuild;
    public Long oldBuild;
    public boolean backupEnabled;
    public int backupTimeoutSeconds;
    public int healthTimeoutSeconds;
    public boolean deletePackageAfterUpgrade;
    public String status;
    public String message;
    public Instant startedAt;
    public String releasePath;
    public String backupPath;
    public Long backupBytes;
    public String backupSha256;
    public Instant backupCompletedAt;
    public Instant finishedAt;
  }

  // 不声明主键属性，让 EntityManager 将两个 @UpdateBy 字段都用于 WHERE。
  @Table("t_upgrade_record")
  static class Claim {
    @UpdateBy
    @Column("id")
    public long recordId;
    @UpdateBy
    @Column("status")
    public String expectedStatus;
    public String status;
    public String message;
  }

  @Table("t_upgrade_path_mapping")
  static class MappingRow {
    public long recordId;
    public String name;
    public String mode;
    public String source;
    public String target;
  }

  @Table("t_backup")
  static class BackupRow {
    @GeneratedId
    public Long id;
    public String name;
    public String status;
    public String version;
    public String build;
    public Instant createdAt;
    public boolean protectedBackup;
    public String manifestSha256;
    public long totalBytes;
    public long sourceOperationId;
    public String location;
    public String scope;
  }

  @Table("t_upgrade_package")
  static class PackageRow {
    @Id
    public Long id;
  }

}
