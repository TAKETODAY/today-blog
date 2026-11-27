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

package cn.taketoday.blog.maintenance.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.problem.CommonProblems;
import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.updater.BackupJob;
import cn.taketoday.blog.updater.BackupType;
import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.updater.UpgradeFiles;
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

  private static final Path SUBMIT = Path.of("/usr/local/lib/today-blog/backup-submit");

  private final EntityManager entityManager;

  private final DatabaseBackupFactory archives;

  public BackupService(EntityManager entityManager, DatabaseBackupFactory archives) {
    this.entityManager = entityManager;
    this.archives = archives;
  }

  public Map<String, Object> capabilities() {
    Properties policy = consolePolicy();
    if (policy != null) {
      return Map.of("create", true, "restore", false, "delete", false, "protect", false,
              "types", BackupType.parse(policy.getProperty("types")),
              "offsite", "true".equals(policy.getProperty("offsite")));
    }
    // 在独立执行器具备完整校验和停写能力之前，后端也必须拒绝危险操作。
    return Map.of("create", false,
            "restore", false,
            "delete", false,
            "protect", false,
            "reason", "备份与恢复执行器尚未接通，暂不能创建、保护、删除或恢复备份。仅备份数据库不等于完整备份。"
    );
  }

  private Properties consolePolicy() {
    try {
      if (System.getenv("BLOG_RELEASE_PATH") == null || !"systemd".equals(
              Files.readString(Path.of("/etc/today-blog/service-manager")).strip())) {
        return null;
      }
      var files = new UpgradeFiles();
      files.verifyTrusted(BackupJob.POLICY, false);
      files.verifyTrusted(SUBMIT, false);
      files.verifyTrusted(BackupJob.STATUS, false);
      if (!Files.isExecutable(SUBMIT)) {
        return null;
      }
      Properties policy = files.read(BackupJob.POLICY);
      BackupType.parse(policy.getProperty("types", ""));
      return policy;
    }
    catch (IOException | IllegalArgumentException | UnsupportedOperationException failure) {
      return null;
    }
  }

  public BackupJob submit(Set<BackupType> types, boolean offsite) {
    Properties policy = consolePolicy();
    if (policy == null) {
      throw new ProblemException(BackupProblems.EXECUTOR_UNAVAILABLE);
    }
    if (types == null || types.isEmpty() || !BackupType.parse(policy.getProperty("types")).containsAll(types)
            || (offsite && !"true".equals(policy.getProperty("offsite")))) {
      throw new ProblemException(CommonProblems.INVALID_REQUEST);
    }
    BackupJob job = BackupJob.create(types, offsite);
    try {
      Process process = new ProcessBuilder("/usr/bin/sudo", "-n", "--", SUBMIT.toString(), job.encode())
              .redirectInput(Path.of("/dev/null").toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      try {
        if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
          throw new ProblemException(BackupProblems.EXECUTOR_UNAVAILABLE);
        }
      }
      finally {
        if (process.isAlive()) {
          process.destroyForcibly();
        }
      }
      return job;
    }
    catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new ProblemException(BackupProblems.EXECUTOR_UNAVAILABLE);
    }
    catch (IOException failure) {
      throw new ProblemException(BackupProblems.EXECUTOR_UNAVAILABLE);
    }
  }

  public List<JobView> jobs() {
    var files = new UpgradeFiles();
    try {
      files.verifyTrusted(BackupJob.STATUS, false);
      try (var paths = Files.list(BackupJob.STATUS)) {
        return paths.filter(path -> path.getFileName().toString().matches("[0-9a-f]{32}\\.properties"))
                .map(this::readJob).filter(Objects::nonNull)
                .sorted(Comparator.comparing(JobView::createdAt).reversed()).limit(LIST_LIMIT).toList();
      }
    }
    catch (IOException | UnsupportedOperationException failure) {
      if (Files.notExists(BackupJob.STATUS)) {
        return List.of();
      }
      throw new ProblemException(BackupProblems.EXECUTOR_UNAVAILABLE);
    }
  }

  private JobView readJob(Path path) {
    try {
      var files = new UpgradeFiles();
      files.verifyTrusted(path, false);
      Properties record = files.read(path);
      Instant created = Instant.parse(record.getProperty("createdAt"));
      String status = record.getProperty("status");
      Instant now = Instant.now();
      if (("STARTING".equals(status) && created.plusSeconds(120).isBefore(now))
              || ("CREATING".equals(status) && created.plusSeconds(18120).isBefore(now))) {
        status = "MAINTENANCE_REQUIRED";
      }
      return new JobView(record.getProperty("id"), record.getProperty("types"), status, created,
              record.getProperty("completedAt"), Boolean.parseBoolean(record.getProperty("offsite")),
              record.getProperty("archive"), Long.parseLong(record.getProperty("bytes", "0")));
    }
    catch (IOException | RuntimeException failure) {
      return null;
    }
  }

  public record JobView(String id, String types, String status, Instant createdAt, String completedAt,
          boolean offsite, String archive, long bytes) {
  }

  public List<BackupView> backups() {
    return entityManager.slice(Backup.class, new LatestBackups(), Pageable.of(1, LIST_LIMIT))
            .rows()
            .stream()
            .map(BackupView::from)
            .toList();
  }

  public BackupDetail backup(long id) {
    Backup record = requireBackup(id);
    return new BackupDetail(BackupView.from(record), record.getScope(),
            record.getRestoreVerifiedAt(), record.getLocation());
  }

  public Download download(long id) {
    Backup record = requireBackup(id);
    String location = record.getLocation();
    if (record.getStatus() != BackupStatus.AVAILABLE || location == null) {
      throw new ProblemException(BackupProblems.DOWNLOAD_UNAVAILABLE);
    }
    try {
      Path archive = archives.archive(location);
      return new Download(archive, Files.size(archive));
    }
    catch (IOException | IllegalArgumentException ex) {
      throw new ProblemException(BackupProblems.DOWNLOAD_UNAVAILABLE);
    }
  }

  private Backup requireBackup(long id) {
    Backup record = entityManager.findById(Backup.class, id);
    if (record == null) {
      throw new ProblemException(BackupProblems.NOT_FOUND);
    }
    return record;
  }

  public void requireExecutor() {
    throw new ProblemException(BackupProblems.EXECUTOR_UNAVAILABLE);
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
          Long id,
          String name,
          BackupStatus status,
          String version,
          String build,
          Instant createdAt,
          boolean protectedBackup,
          long totalBytes,
          String manifestSha256,
          Long sourceOperationId) {

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

  public record Download(Path archive, long bytes) {
  }

}
