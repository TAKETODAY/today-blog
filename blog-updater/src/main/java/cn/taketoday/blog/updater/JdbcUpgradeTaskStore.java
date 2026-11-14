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

package cn.taketoday.blog.updater;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 升级器直接读写数据库任务状态。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public final class JdbcUpgradeTaskStore implements UpgradeTaskStore {

  private final Connection connection;

  private JdbcUpgradeTaskStore(Connection connection) {
    this.connection = connection;
  }

  public static JdbcUpgradeTaskStore open(UpgradeDatabase database) throws Exception {
    return new JdbcUpgradeTaskStore(
            DriverManager.getConnection(database.url(), database.user(), database.password()));
  }

  @Override
  public UpgradeTask load(String id) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
            select package_id, package_path, package_version, package_build, old_build,
                   backup_enabled, backup_timeout_seconds, health_timeout_seconds,
                   delete_package_after_upgrade
            from t_upgrade_record where id = ?
            """)) {
      statement.setString(1, id);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new IllegalStateException("升级任务不存在: " + id);
        }
        Long oldBuild = result.getObject("old_build", Long.class);
        return new UpgradeTask(id,
                result.getString("package_id"),
                result.getString("package_path"),
                result.getString("package_version"),
                result.getLong("package_build"),
                oldBuild,
                result.getBoolean("backup_enabled"),
                result.getInt("backup_timeout_seconds"),
                result.getInt("health_timeout_seconds"),
                result.getBoolean("delete_package_after_upgrade"),
                mappings(id));
      }
    }
  }

  private List<ReleaseMapping> mappings(String id) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
            select name, mode, source, target from t_upgrade_path_mapping
            where record_id = ? order by name
            """)) {
      statement.setString(1, id);
      try (ResultSet result = statement.executeQuery()) {
        List<ReleaseMapping> mappings = new ArrayList<>();
        while (result.next()) {
          mappings.add(new ReleaseMapping(result.getString("name"),
                  ReleaseMapping.Mode.valueOf(result.getString("mode")),
                  result.getString("source"), result.getString("target")));
        }
        return List.copyOf(mappings);
      }
    }
  }

  @Override
  public void status(String id, String status, String message) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
            "update t_upgrade_record set status = ?, message = ? where id = ?")) {
      statement.setString(1, status);
      statement.setString(2, message);
      statement.setString(3, id);
      statement.executeUpdate();
    }
  }

  @Override
  public void started(String id, String releasePath) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
            "update t_upgrade_record set started_at = ?, release_path = ? where id = ?")) {
      statement.setTimestamp(1, Timestamp.from(Instant.now()));
      statement.setString(2, releasePath);
      statement.setString(3, id);
      statement.executeUpdate();
    }
  }

  @Override
  public void backup(String id, DatabaseBackupResult backup) throws Exception {
    String version;
    long build;
    try (PreparedStatement query = connection.prepareStatement(
            "select package_version, package_build from t_upgrade_record where id = ?")) {
      query.setString(1, id);
      try (ResultSet result = query.executeQuery()) {
        if (!result.next()) {
          throw new IllegalStateException("升级任务不存在: " + id);
        }
        version = result.getString("package_version");
        build = result.getLong("package_build");
      }
    }
    try (PreparedStatement insert = connection.prepareStatement("""
            insert into t_backup (id, name, status, version, build, created_at, protected_backup,
                    manifest_sha256, total_bytes, source_operation_id, location, scope)
            values (?, ?, 'AVAILABLE', ?, ?, ?, false, ?, ?, ?, ?, '数据库')
            """)) {
      insert.setString(1, backup.id());
      insert.setString(2, "升级前数据库备份 " + version);
      insert.setString(3, version);
      insert.setString(4, Long.toString(build));
      insert.setTimestamp(5, Timestamp.from(backup.completedAt()));
      insert.setString(6, backup.sha256());
      insert.setLong(7, backup.bytes());
      insert.setString(8, id);
      insert.setString(9, backup.location().toString());
      insert.executeUpdate();
    }
    try (PreparedStatement update = connection.prepareStatement("""
            update t_upgrade_record
            set backup_path = ?, backup_bytes = ?, backup_sha256 = ?, backup_completed_at = ?
            where id = ?
            """)) {
      update.setString(1, backup.location().toString());
      update.setLong(2, backup.bytes());
      update.setString(3, backup.sha256());
      update.setTimestamp(4, Timestamp.from(backup.completedAt()));
      update.setString(5, id);
      update.executeUpdate();
    }
  }

  @Override
  public void finished(String id, String status, String message) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement("""
            update t_upgrade_record set status = ?, message = ?, finished_at = ? where id = ?
            """)) {
      statement.setString(1, status);
      statement.setString(2, message);
      statement.setTimestamp(3, Timestamp.from(Instant.now()));
      statement.setString(4, id);
      statement.executeUpdate();
    }
  }

  @Override
  public void deletePackage(String packageId) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(
            "delete from t_upgrade_package where id = ?")) {
      statement.setString(1, packageId);
      statement.executeUpdate();
    }
  }

  @Override
  public void close() throws Exception {
    connection.close();
  }

}
