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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.web.ErrorMessageException;
import infra.beans.factory.annotation.Autowired;
import infra.http.HttpStatus;
import infra.persistence.EntityManager;
import infra.persistence.Pageable;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.In;
import infra.persistence.annotation.OR;
import infra.persistence.annotation.OrderByClause;
import infra.persistence.annotation.WhereIsNull;
import infra.stereotype.Service;

/**
 * 备份管理查询不将数据库索引等同于可恢复的磁盘备份。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@Service
public class BackupService {

  private static final int LIST_LIMIT = 500;

  private final EntityManager entityManager;

  private final BackupIndexService index;

  public BackupService(EntityManager entityManager) {
    this(entityManager, new BackupIndexService(entityManager, new BackupOperationService(entityManager)));
  }

  @Autowired
  public BackupService(EntityManager entityManager, BackupIndexService index) {
    this.entityManager = entityManager;
    this.index = index;
  }

  public Map<String, Object> capabilities() {
    // 在独立执行器具备完整校验和停写能力之前，后端也必须拒绝危险操作。
    return Map.of("create", false,
            "restore", false,
            "delete", false,
            "protect", false,
            "reason", "备份与恢复执行器尚未接通，暂不能创建、保护、删除或恢复备份。仅备份数据库不等于完整备份。"
    );
  }

  public List<BackupView> backups() {
    synchronizeIndex();
    return entityManager.slice(Backup.class, new LatestBackups(), Pageable.of(1, LIST_LIMIT))
            .rows()
            .stream()
            .map(BackupView::from)
            .toList();
  }

  public BackupDetail backup(String id) {
    UpgradeFiles.checkId(id);
    synchronizeIndex();
    Backup record = entityManager.findById(Backup.class, id);
    if (record == null) {
      throw ErrorMessageException.failed("备份不存在", HttpStatus.NOT_FOUND);
    }
    // 核心字段取结构化列，metadata 只补充不属于列的展示信息，旧记录不能覆盖保护状态。
    Properties metadata = decodeMetadata(record.getMetadata());
    return new BackupDetail(
            BackupView.from(record),
            metadata.getProperty("scope"),
            metadata.getProperty("restoreVerifiedAt"));
  }

  public List<OperationView> operations() {
    synchronizeIndex();
    return entityManager.slice(BackupOperation.class, new BackupOperations(), Pageable.of(1, LIST_LIMIT))
            .rows()
            .stream()
            .map(OperationView::from)
            .toList();
  }

  public void requireExecutor() {
    throw ErrorMessageException.failed("备份与恢复执行器尚未接通，未执行任何备份操作", HttpStatus.SERVICE_UNAVAILABLE);
  }

  private void synchronizeIndex() {
    try {
      index.synchronize();
    }
    catch (IOException | IllegalArgumentException ex) {
      // 不把同步失败伪装成空列表，也不向客户端暴露磁盘路径或原始记录。
      throw ErrorMessageException.failed("备份磁盘记录同步失败，请联系管理员检查执行日志", HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  private static Properties decodeMetadata(String value) {
    try {
      return BackupOperationService.decode(value);
    }
    catch (IllegalStateException ex) {
      throw new IllegalStateException("备份元数据格式错误", ex);
    }
  }

  /**
   * 最近备份：只声明排序，不产生 WHERE 条件。
   */
  @OrderByClause("created_at DESC, id DESC")
  @EntityRef(Backup.class)
  static class LatestBackups {

  }

  /**
   * 备份相关操作记录：备份操作类型，或关联了备份、保护备份的系统升级。
   */
  @OrderByClause("created_at DESC, id DESC")
  @EntityRef(BackupOperation.class)
  static class BackupOperations {

    @In
    public final List<BackupOperationType> operationType = List.of(
            BackupOperationType.BACKUP_CREATE,
            BackupOperationType.BACKUP_RESTORE,
            BackupOperationType.BACKUP_DELETE,
            BackupOperationType.BACKUP_PROTECT);

    @OR
    @WhereIsNull(not = true)
    public final @Nullable String backupId = null;

    @OR
    @WhereIsNull(not = true)
    public final @Nullable String protectionBackupId = null;
  }

  /**
   * 备份列表和详情共用的只读视图，不暴露存储列 metadata。
   */
  public record BackupView(
          String id,
          String name,
          BackupStatus status,
          String version,
          String build,
          Long operatorId,
          String createdAt,
          boolean protectedBackup,
          long totalBytes,
          String manifestSha256,
          String sourceOperationId) {

    static BackupView from(Backup backup) {
      return new BackupView(
              backup.getId(),
              backup.getName(),
              backup.getStatus(),
              backup.getVersion(),
              backup.getBuild(),
              backup.getOperatorId(),
              backup.getCreatedAt(),
              backup.isProtectedBackup(),
              backup.getTotalBytes(),
              backup.getManifestSha256(),
              backup.getSourceOperationId());
    }
  }

  /**
   * 备份详情在基础视图之外补充 metadata 中的展示字段。
   */
  public record BackupDetail(BackupView backup, String scope, String restoreVerifiedAt) {
  }

  /**
   * 备份操作记录视图，字段名与前端契约一致。
   */
  public record OperationView(
          String id,
          BackupOperationType type,
          BackupOperationStatus status,
          Long operatorId,
          String createdAt,
          String finishedAt,
          String backupId,
          String protectionBackupId) {

    static OperationView from(BackupOperation operation) {
      return new OperationView(
              operation.getId(),
              operation.getOperationType(),
              operation.getStatus(),
              operation.getOperatorId(),
              operation.getCreatedAt(),
              operation.getFinishedAt(),
              operation.getBackupId(),
              operation.getProtectionBackupId());
    }
  }

}
