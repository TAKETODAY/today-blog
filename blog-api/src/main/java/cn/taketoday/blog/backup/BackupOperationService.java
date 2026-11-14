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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradeStore;
import infra.beans.factory.annotation.Autowired;
import infra.persistence.EntityManager;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.OrderByClause;
import infra.stereotype.Service;

/**
 * 数据库保存查询索引，磁盘协议保留独立执行和恢复依据。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@Service
public class BackupOperationService {

  private final EntityManager entityManager;

  private final UpgradeStore upgrades;

  @Autowired
  public BackupOperationService(EntityManager entityManager) {
    this(entityManager, new UpgradeStore(Path.of(System.getenv().getOrDefault(
            "BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade"))));
  }

  BackupOperationService(EntityManager entityManager, UpgradeStore upgrades) {
    this.entityManager = entityManager;
    this.upgrades = upgrades;
  }

  public synchronized List<Properties> upgradeHistory() throws IOException {
    // 恢复数据库会回退索引，按固定 UUID 补齐，不能仅依赖内存同步游标。
    for (Properties record : upgrades.history()) {
      save(record, BackupOperationType.UPGRADE);
    }

    List<BackupOperation> records = entityManager.find(BackupOperation.class, new UpgradeOperations());

    List<Properties> history = new ArrayList<>(records.size());
    for (BackupOperation record : records) {
      history.add(decode(record.getMetadata()));
    }
    return history;
  }

  public synchronized Properties upgrade(String id) throws IOException {
    UpgradeFiles.checkId(id);
    save(upgrades.task(id), BackupOperationType.UPGRADE);
    BackupOperation record = entityManager.findById(BackupOperation.class, id);
    if (record == null) {
      throw new IllegalStateException("升级记录不存在：" + id);
    }
    return decode(record.getMetadata());
  }

  public synchronized void recordUpgrade(String id) throws IOException {
    UpgradeFiles.checkId(id);
    save(upgrades.task(id), BackupOperationType.UPGRADE);
  }

  void save(Properties record, BackupOperationType type) throws IOException {
    String id = record.getProperty("id");
    UpgradeFiles.checkId(id);
    BackupOperation operation = new BackupOperation();
    operation.setId(id);
    operation.setOperationType(type);
    operation.setStatus(BackupOperationStatus.of(record.getProperty("status")));
    operation.setOperatorId(parseOperatorId(record.getProperty("operatorId")));
    operation.setCreatedAt(record.getProperty("createdAt", record.getProperty("startedAt", "")));
    operation.setFinishedAt(record.getProperty("finishedAt"));
    operation.setBackupId(record.getProperty("backupId"));
    operation.setProtectionBackupId(record.getProperty("protectionBackupId"));
    operation.setMetadata(encode(record));
    // 恢复数据库会丢失索引，磁盘记录需要重新补齐，因此按主键覆盖已有行。
    if (entityManager.findById(BackupOperation.class, id) == null) {
      entityManager.persist(operation);
    }
    else {
      // 覆盖整行，避免数据库保留磁盘记录中已不存在的旧状态。
      entityManager.updateById(operation, PropertyUpdateStrategy.always());
    }
  }

  static String encode(Properties properties) throws IOException {
    StringWriter writer = new StringWriter();
    properties.store(writer, null);
    return writer.toString();
  }

  static Properties decode(String value) {
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(value));
      return properties;
    }
    catch (IOException | IllegalArgumentException invalid) {
      throw new IllegalStateException("操作记录格式错误", invalid);
    }
  }

  private static Long parseOperatorId(String value) {
    return value == null || value.isBlank() ? null : Long.valueOf(value);
  }

  /**
   * 系统升级记录，按创建时间倒序。
   */
  @OrderByClause("created_at DESC")
  @EntityRef(BackupOperation.class)
  static class UpgradeOperations {

    public final BackupOperationType operationType = BackupOperationType.UPGRADE;
  }

}
