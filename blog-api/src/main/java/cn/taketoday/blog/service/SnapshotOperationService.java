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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeStore;
import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.stereotype.Service;

/**
 * 数据库保存查询索引，磁盘协议保留独立执行和恢复依据。
 */
@Service
public class SnapshotOperationService {

  private final JdbcTemplate jdbc;

  private final UpgradeStore upgrades;

  @Autowired
  public SnapshotOperationService(JdbcTemplate jdbc) {
    this(jdbc, new UpgradeStore(Path.of(System.getenv().getOrDefault(
            "BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"))));
  }

  SnapshotOperationService(JdbcTemplate jdbc, UpgradeStore upgrades) {
    this.jdbc = jdbc;
    this.upgrades = upgrades;
  }

  public synchronized List<Properties> upgradeHistory() throws IOException {
    // 恢复数据库会回退索引，按固定 UUID 补齐，不能仅依赖内存同步游标。
    for (Properties record : upgrades.history()) {
      save(record, "UPGRADE");
    }
    return jdbc.query("SELECT metadata FROM t_app_operation WHERE operation_type = 'UPGRADE' ORDER BY created_at DESC",
            (rs, row) -> decode(rs.getString(1)));
  }

  public synchronized Properties upgrade(String id) throws IOException {
    UpgradeFiles.checkId(id);
    save(upgrades.task(id), "UPGRADE");
    return jdbc.queryForObject("SELECT metadata FROM t_app_operation WHERE id = ? AND operation_type = 'UPGRADE'",
            (rs, row) -> decode(rs.getString(1)), id);
  }

  public synchronized void recordUpgrade(String id) throws IOException {
    UpgradeFiles.checkId(id);
    save(upgrades.task(id), "UPGRADE");
  }

  void save(Properties record, String type) throws IOException {
    String id = record.getProperty("id");
    UpgradeFiles.checkId(id);
    jdbc.update("""
            INSERT INTO t_app_operation
              (id, operation_type, status, operator_id, created_at, finished_at, snapshot_id, protection_snapshot_id, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE status = VALUES(status), operator_id = VALUES(operator_id),
              created_at = VALUES(created_at), finished_at = VALUES(finished_at), snapshot_id = VALUES(snapshot_id),
              protection_snapshot_id = VALUES(protection_snapshot_id), metadata = VALUES(metadata)
            """, id, type, record.getProperty("status"), record.getProperty("operatorId"),
            record.getProperty("createdAt", record.getProperty("startedAt", "")), record.getProperty("finishedAt"),
            record.getProperty("snapshotId"), record.getProperty("protectionSnapshotId"), encode(record));
  }

  static String encode(Properties properties) throws IOException {
    StringWriter writer = new StringWriter();
    properties.store(writer, null);
    return writer.toString();
  }

  static Properties decode(String value) throws java.sql.SQLException {
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(value));
      return properties;
    }
    catch (IOException | IllegalArgumentException invalid) {
      throw new java.sql.SQLException("操作记录格式错误", invalid);
    }
  }
}
