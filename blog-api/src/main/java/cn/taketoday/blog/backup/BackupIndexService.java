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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;
import infra.beans.factory.annotation.Autowired;
import infra.persistence.EntityManager;
import infra.persistence.PropertyUpdateStrategy;
import infra.stereotype.Service;

/**
 * 从独立执行器的磁盘记录重建查询索引，不读取备份内容或执行恢复。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/13
 */
@Service
public class BackupIndexService {

  private final EntityManager entityManager;

  private final BackupOperationService operations;

  private final Path state;

  @Autowired
  public BackupIndexService(EntityManager entityManager, BackupOperationService operations) {
    this(entityManager, operations, Path.of(System.getenv().getOrDefault(
            "BLOG_UPGRADE_STATE", "/var/lib/today-blog-upgrade")));
  }

  BackupIndexService(EntityManager entityManager, BackupOperationService operations, Path state) {
    this.entityManager = entityManager;
    this.operations = operations;
    this.state = state;
  }

  public synchronized void synchronize() throws IOException {
    // 每次回放完整历史，不能用内存游标跳过数据库恢复后丢失的记录。
    List<Backup> backups = new ArrayList<>();
    for (Properties record : records("backups")) {
      backups.add(backup(record));
    }
    List<Properties> history = records("backup-operations");
    // 先校验整个批次，避免未知状态或损坏记录被静默降级为可用状态。
    for (Properties record : history) {
      BackupOperationType type = BackupOperationType.valueOf(required(record, "operationType"));
      if (type == BackupOperationType.UPGRADE) {
        throw new IllegalArgumentException("备份操作目录不能保存升级记录");
      }
      if (BackupOperationStatus.of(required(record, "status")) == null) {
        throw new IllegalArgumentException("未知的备份操作状态");
      }
      required(record, "createdAt");
      operator(record);
      optionalId(record, "backupId");
      optionalId(record, "protectionBackupId");
    }
    for (Backup backup : backups) {
      if (entityManager.findById(Backup.class, backup.getId()) == null) {
        entityManager.persist(backup);
      }
      else {
        // 包括删除墓碑和取消保护，不能保留恢复后的旧列值。
        entityManager.updateById(backup, PropertyUpdateStrategy.always());
      }
    }
    for (Properties record : history) {
      operations.save(record, BackupOperationType.valueOf(record.getProperty("operationType")));
    }
    // 关联保护备份的升级记录也需要回放，不能要求先访问升级页面才补齐。
    operations.upgradeHistory();
  }

  private List<Properties> records(String directory) throws IOException {
    Path root = state.resolve(directory);
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      return List.of();
    }
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("备份索引目录不是普通目录");
    }
    List<Properties> result = new ArrayList<>();
    try (var paths = Files.list(root)) {
      for (Path path : paths.sorted().toList()) {
        String name = path.getFileName().toString();
        // 执行器以原子重命名发布记录，未发布的临时文件不参与回放。
        if (!name.endsWith(".properties")) {
          continue;
        }
        String id = name.substring(0, name.length() - ".properties".length());
        UpgradeFiles.checkId(id);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
          throw new IOException("备份索引记录不是普通文件");
        }
        Properties record = UpgradeFiles.read(path);
        if (!id.equals(required(record, "id"))) {
          throw new IllegalArgumentException("备份索引文件名与记录 ID 不一致");
        }
        result.add(record);
      }
    }
    return result;
  }

  private static Backup backup(Properties record) throws IOException {
    Backup backup = new Backup();
    backup.setId(required(record, "id"));
    backup.setName(required(record, "name"));
    backup.setStatus(BackupStatus.valueOf(required(record, "status")));
    backup.setVersion(required(record, "version"));
    backup.setBuild(required(record, "build"));
    backup.setCreatedAt(required(record, "createdAt"));
    backup.setOperatorId(operator(record));
    String protection = required(record, "protectedBackup");
    if (!"true".equals(protection) && !"false".equals(protection)) {
      throw new IllegalArgumentException("备份保护状态无效");
    }
    backup.setProtectedBackup(Boolean.parseBoolean(protection));
    long totalBytes = Long.parseLong(required(record, "totalBytes"));
    if (totalBytes < 0) {
      throw new IllegalArgumentException("备份大小无效");
    }
    backup.setTotalBytes(totalBytes);
    String digest = record.getProperty("manifestSha256");
    if (backup.getStatus() == BackupStatus.AVAILABLE
            && (digest == null || !digest.matches("[0-9a-f]{64}"))) {
      throw new IllegalArgumentException("可用备份缺少有效清单摘要");
    }
    backup.setManifestSha256(digest);
    optionalId(record, "sourceOperationId");
    backup.setSourceOperationId(record.getProperty("sourceOperationId"));
    backup.setMetadata(BackupOperationService.encode(record));
    return backup;
  }

  private static Long operator(Properties record) {
    String value = record.getProperty("operatorId");
    return value == null || value.isBlank() ? null : Long.valueOf(value);
  }

  private static void optionalId(Properties record, String key) {
    String value = record.getProperty(key);
    if (value != null) {
      UpgradeFiles.checkId(value);
    }
  }

  private static String required(Properties record, String key) {
    String value = record.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("备份索引缺少字段：" + key);
    }
    return value;
  }
}
