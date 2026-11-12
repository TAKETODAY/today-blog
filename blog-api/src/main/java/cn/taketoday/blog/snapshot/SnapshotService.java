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

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.web.ErrorMessageException;
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
 * 快照管理查询不将数据库索引等同于可恢复的磁盘备份。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@Service
public class SnapshotService {

  private static final int LIST_LIMIT = 500;

  private final EntityManager entityManager;

  public SnapshotService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public Map<String, Object> capabilities() {
    // 在独立执行器具备完整校验和停写能力之前，后端也必须拒绝危险操作。
    return Map.of("create", false,
            "restore", false,
            "delete", false,
            "protect", false,
            "reason", "完整快照执行器尚未接通，暂不能创建、保护、删除或恢复快照。数据库备份不等于完整应用快照。"
    );
  }

  public List<SnapshotView> snapshots() {
    return entityManager.slice(AppSnapshot.class, new LatestSnapshots(), Pageable.of(1, LIST_LIMIT))
            .rows()
            .stream()
            .map(SnapshotView::from)
            .toList();
  }

  public SnapshotDetail snapshot(String id) {
    UpgradeFiles.checkId(id);
    AppSnapshot record = entityManager.findById(AppSnapshot.class, id);
    if (record == null) {
      throw ErrorMessageException.failed("应用快照不存在", HttpStatus.NOT_FOUND);
    }
    // 核心字段取结构化列，metadata 只补充不属于列的展示信息，旧记录不能覆盖保护状态。
    Properties metadata = decodeMetadata(record.getMetadata());
    return new SnapshotDetail(
            SnapshotView.from(record),
            metadata.getProperty("scope"),
            metadata.getProperty("restoreVerifiedAt"));
  }

  public List<OperationView> operations() {
    return entityManager.slice(AppOperation.class, new SnapshotOperations(), Pageable.of(1, LIST_LIMIT))
            .rows()
            .stream()
            .map(OperationView::from)
            .toList();
  }

  public void requireExecutor() {
    throw ErrorMessageException.failed("完整快照执行器尚未接通，未执行任何快照操作", HttpStatus.SERVICE_UNAVAILABLE);
  }

  private static Properties decodeMetadata(String value) {
    try {
      return SnapshotOperationService.decode(value);
    }
    catch (IllegalStateException ex) {
      throw new IllegalStateException("快照元数据格式错误", ex);
    }
  }

  /**
   * 最近快照：只声明排序，不产生 WHERE 条件。
   */
  @OrderByClause("created_at DESC, id DESC")
  @EntityRef(AppSnapshot.class)
  static class LatestSnapshots {

  }

  /**
   * 快照相关操作记录：快照操作类型，或关联了快照、保护快照的系统更新。
   */
  @OrderByClause("created_at DESC, id DESC")
  @EntityRef(AppOperation.class)
  static class SnapshotOperations {

    @In
    public final List<AppOperationType> operationType = List.of(
            AppOperationType.SNAPSHOT_CREATE,
            AppOperationType.SNAPSHOT_RESTORE,
            AppOperationType.SNAPSHOT_DELETE,
            AppOperationType.SNAPSHOT_PROTECT);

    @OR
    @WhereIsNull(not = true)
    public final @Nullable String snapshotId = null;

    @OR
    @WhereIsNull(not = true)
    public final @Nullable String protectionSnapshotId = null;
  }

  /**
   * 快照列表和详情共用的只读视图，不暴露存储列 metadata。
   */
  public record SnapshotView(
          String id,
          String name,
          AppSnapshotStatus status,
          String version,
          String build,
          Long operatorId,
          String createdAt,
          boolean protectedSnapshot,
          long totalBytes,
          String manifestSha256,
          String sourceOperationId) {

    static SnapshotView from(AppSnapshot snapshot) {
      return new SnapshotView(
              snapshot.getId(),
              snapshot.getName(),
              snapshot.getStatus(),
              snapshot.getVersion(),
              snapshot.getBuild(),
              snapshot.getOperatorId(),
              snapshot.getCreatedAt(),
              snapshot.isProtectedSnapshot(),
              snapshot.getTotalBytes(),
              snapshot.getManifestSha256(),
              snapshot.getSourceOperationId());
    }
  }

  /**
   * 快照详情在基础视图之外补充 metadata 中的展示字段。
   */
  public record SnapshotDetail(SnapshotView snapshot, String scope, String restoreVerifiedAt) {
  }

  /**
   * 快照操作记录视图，字段名与前端契约一致。
   */
  public record OperationView(
          String id,
          AppOperationType type,
          AppOperationStatus status,
          Long operatorId,
          String createdAt,
          String finishedAt,
          String snapshotId,
          String protectionSnapshotId) {

    static OperationView from(AppOperation operation) {
      return new OperationView(
              operation.getId(),
              operation.getOperationType(),
              operation.getStatus(),
              operation.getOperatorId(),
              operation.getCreatedAt(),
              operation.getFinishedAt(),
              operation.getSnapshotId(),
              operation.getProtectionSnapshotId());
    }
  }

}
