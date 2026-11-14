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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.web.ErrorMessageException;
import infra.http.HttpStatus;
import infra.persistence.EntityManager;
import infra.persistence.Pageable;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.OrderByClause;
import infra.stereotype.Service;

/**
 * 系统备份查询。数据库保存备份元信息，{@code location} 指向本地文件。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@Service
public class BackupService {

  private static final int LIST_LIMIT = 500;

  private final EntityManager entityManager;

  public BackupService(EntityManager entityManager) {
    this.entityManager = entityManager;
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
    return entityManager.slice(Backup.class, new LatestBackups(), Pageable.of(1, LIST_LIMIT))
            .rows()
            .stream()
            .map(BackupView::from)
            .toList();
  }

  public BackupDetail backup(String id) {
    UpgradeFiles.checkId(id);
    Backup record = entityManager.findById(Backup.class, id);
    if (record == null) {
      throw ErrorMessageException.failed("备份不存在", HttpStatus.NOT_FOUND);
    }
    return new BackupDetail(BackupView.from(record), record.getScope(),
            record.getRestoreVerifiedAt(), record.getLocation());
  }

  public void requireExecutor() {
    throw ErrorMessageException.failed("备份与恢复执行器尚未接通，未执行任何备份操作", HttpStatus.SERVICE_UNAVAILABLE);
  }

  /**
   * 最近备份：只声明排序，不产生 WHERE 条件。
   */
  @OrderByClause("created_at DESC, id DESC")
  @EntityRef(Backup.class)
  static class LatestBackups {

  }

  /**
   * 备份列表只展示结构化字段。
   */
  public record BackupView(
          String id,
          String name,
          BackupStatus status,
          String version,
          String build,
          Instant createdAt,
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
              backup.getCreatedAt(),
              backup.isProtectedBackup(),
              backup.getTotalBytes(),
              backup.getManifestSha256(),
              backup.getSourceOperationId());
    }
  }

  /**
   * 备份详情补充覆盖范围、恢复验证记录和本地文件路径。
   */
  public record BackupDetail(BackupView backup, String scope, Instant restoreVerifiedAt, String location) {
  }

}
