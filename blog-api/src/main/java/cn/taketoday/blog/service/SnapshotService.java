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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.web.ErrorMessageException;
import infra.http.HttpStatus;
import infra.jdbc.core.JdbcTemplate;
import infra.stereotype.Service;

/**
 * 快照管理查询不将数据库索引等同于可恢复的磁盘备份。
 */
@Service
public class SnapshotService {

  private final JdbcTemplate jdbc;

  public SnapshotService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> capabilities() {
    // 在独立执行器具备完整校验和停写能力之前，后端也必须拒绝危险操作。
    return Map.of("create", false, "restore", false, "delete", false, "protect", false,
            "reason", "完整快照执行器尚未接通，暂不能创建、保护、删除或恢复快照。数据库备份不等于完整应用快照。");
  }

  public List<Properties> snapshots() {
    return jdbc.query("""
            SELECT id, name, status, version, build, operator_id, created_at, protected_snapshot,
              manifest_sha256, total_bytes, source_operation_id
            FROM t_app_snapshot ORDER BY created_at DESC, id DESC LIMIT 500
            """, (rs, row) -> {
      Properties result = new Properties();
      result.setProperty("id", rs.getString("id"));
      result.setProperty("name", rs.getString("name"));
      result.setProperty("status", rs.getString("status"));
      result.setProperty("version", rs.getString("version") == null ? "" : rs.getString("version"));
      result.setProperty("build", rs.getString("build") == null ? "" : rs.getString("build"));
      result.setProperty("operatorId", rs.getString("operator_id") == null ? "" : rs.getString("operator_id"));
      result.setProperty("createdAt", rs.getString("created_at"));
      result.setProperty("protected", Boolean.toString(rs.getBoolean("protected_snapshot")));
      result.setProperty("totalBytes", Long.toString(rs.getLong("total_bytes")));
      result.setProperty("manifestSha256", rs.getString("manifest_sha256") == null ? "" : rs.getString("manifest_sha256"));
      result.setProperty("sourceOperationId", rs.getString("source_operation_id") == null ? "" : rs.getString("source_operation_id"));
      return result;
    });
  }

  public Properties snapshot(String id) {
    UpgradeFiles.checkId(id);
    List<Properties> records = jdbc.query("""
            SELECT metadata, id, name, status, version, build, protected_snapshot, total_bytes,
              manifest_sha256, source_operation_id, created_at, operator_id
            FROM t_app_snapshot WHERE id = ?
            """, (rs, row) -> {
      Properties metadata = SnapshotOperationService.decode(rs.getString("metadata"));
      Properties result = new Properties();
      for (String field : List.of("scope", "restoreVerifiedAt")) {
        String value = metadata.getProperty(field);
        if (value != null) {
          result.setProperty(field, value);
        }
      }
      // 核心字段以结构化列为准，不允许旧 metadata 覆盖保护和状态。
      result.setProperty("id", rs.getString("id"));
      result.setProperty("name", rs.getString("name"));
      result.setProperty("status", rs.getString("status"));
      result.setProperty("protected", Boolean.toString(rs.getBoolean("protected_snapshot")));
      result.setProperty("totalBytes", Long.toString(rs.getLong("total_bytes")));
      for (String[] field : new String[][] {
              { "version", "version" }, { "build", "build" }, { "createdAt", "created_at" },
              { "operatorId", "operator_id" }, { "manifestSha256", "manifest_sha256" },
              { "sourceOperationId", "source_operation_id" } }) {
        String value = rs.getString(field[1]);
        result.setProperty(field[0], value == null ? "" : value);
      }
      return result;
    }, id);
    if (records.isEmpty()) {
      throw ErrorMessageException.failed("应用快照不存在", HttpStatus.NOT_FOUND);
    }
    return records.getFirst();
  }

  public List<Properties> operations() {
    return jdbc.query("""
            SELECT id, operation_type, status, operator_id, created_at, finished_at,
              snapshot_id, protection_snapshot_id
            FROM t_app_operation
            WHERE operation_type IN ('SNAPSHOT_CREATE', 'SNAPSHOT_RESTORE', 'SNAPSHOT_DELETE', 'SNAPSHOT_PROTECT')
              OR snapshot_id IS NOT NULL OR protection_snapshot_id IS NOT NULL
            ORDER BY created_at DESC, id DESC LIMIT 500
            """, (rs, row) -> {
      Properties result = new Properties();
      for (String[] field : new String[][] {
              { "id", "id" }, { "type", "operation_type" }, { "status", "status" },
              { "operatorId", "operator_id" }, { "createdAt", "created_at" }, { "finishedAt", "finished_at" },
              { "snapshotId", "snapshot_id" }, { "protectionSnapshotId", "protection_snapshot_id" } }) {
        String value = rs.getString(field[1]);
        result.setProperty(field[0], value == null ? "" : value);
      }
      return result;
    });
  }

  public void requireExecutor() {
    throw ErrorMessageException.failed("完整快照执行器尚未接通，未执行任何快照操作", HttpStatus.SERVICE_UNAVAILABLE);
  }
}
