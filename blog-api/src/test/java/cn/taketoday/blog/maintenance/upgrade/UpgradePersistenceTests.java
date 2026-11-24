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

package cn.taketoday.blog.maintenance.upgrade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.blog.updater.ReleaseMapping;
import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeOptions;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 升级包、升级记录、路径映射和默认参数的数据库持久化。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@WebAPITest
class UpgradePersistenceTests {

  @Autowired
  EntityManager entityManager;

  @Autowired
  UpgradeRecordService records;

  @Autowired
  UpgradePathMappingService pathMappings;

  @Autowired
  UpgradeSettingService settings;

  @Autowired
  DataSource dataSource;

  @TempDir
  Path temp;

  @Test
  void packageCrudKeepsFileInStepWithRecord() throws Exception {
    RuntimeLayout layout = new RuntimeLayout(temp);
    UpgradePackageService packages = new UpgradePackageService(entityManager, layout);
    String relativeLocation = "upload-id/package.zip";
    Path location = layout.resolvePackage(relativeLocation);
    Files.createDirectories(location.getParent());
    Files.writeString(location, "zip");
    UpgradePackageEntity entity = packageEntity(relativeLocation);

    packages.create(entity);
    assertThat(packages.find(entity.getId()).getDisplayName()).isEqualTo("测试包");
    assertThat(packages.find(entity.getId()).getLocation()).isEqualTo(relativeLocation);

    packages.update(entity.getId(), "新名称", "备注");
    assertThat(packages.find(entity.getId()).getDisplayName()).isEqualTo("新名称");
    assertThat(packages.find(entity.getId()).getNote()).isEqualTo("备注");

    packages.markConsumed(entity.getId());
    assertThat(packages.find(entity.getId()).getStatus()).isEqualTo(UpgradePackageStatus.CONSUMED);

    packages.delete(entity.getId());
    assertThat(entityManager.findById(UpgradePackageEntity.class, entity.getId())).isNull();
    assertThat(Files.exists(location)).isFalse();
  }

  @Test
  void rejectsAbsolutePackagePathsBeforePersisting() {
    RuntimeLayout layout = new RuntimeLayout(temp);
    UpgradePackageService packages = new UpgradePackageService(entityManager, layout);
    String absolute = layout.packages().resolve("upload-id/package.zip").toString();
    UpgradePackageEntity entity = packageEntity(absolute);
    UpgradeRecordEntity record = recordEntity();
    record.setPackagePath(absolute);

    assertThatIllegalArgumentException().isThrownBy(() -> packages.create(entity));
    assertThatIllegalArgumentException().isThrownBy(() -> records.create(record));
  }

  @Test
  void migrationConvertsExistingFilePathsWithoutChangingAttachmentUris() throws Exception {
    try (var connection = dataSource.getConnection();
            var sql = connection.createStatement();
            var migration = getClass().getResourceAsStream("/db/migration/v3.8__relative-file-paths.sql")) {
      connection.setAutoCommit(false);
      try {
        sql.executeUpdate("insert into attachment (uri, location) values ('/upload/photo.png', '/upload/photo.png')");
        sql.executeUpdate("""
                insert into t_upgrade_record (package_id, package_name, package_version, package_build,
                    package_sha256, package_path, release_path, backup_path, status, created_at)
                values (42, 'legacy-path-test', '3.5', 10, repeat('a', 64), '/srv/old/packages/uuid/package.zip',
                    '/opt/today-blog/releases/3.5-10', '/srv/backup-root/42/database.sql.gz', 'SUCCESS', now())
                """);
        for (String statement : new String(migration.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
          if (!statement.isBlank()) {
            sql.executeUpdate(statement);
          }
        }
        assertThat(value(sql, "select location from attachment where uri = '/upload/photo.png'"))
                .isEqualTo("upload/photo.png");
        assertThat(value(sql, """
                select concat_ws('|', package_path, release_path, backup_path)
                from t_upgrade_record where package_name = 'legacy-path-test'
                """)).isEqualTo("uuid/package.zip|releases/3.5-10|42/database.sql.gz");
      }
      finally {
        connection.rollback();
      }
    }
  }

  private static String value(Statement sql, String query) throws Exception {
    try (var rows = sql.executeQuery(query)) {
      assertThat(rows.next()).isTrue();
      return rows.getString(1);
    }
  }

  @Test
  void recordsSnapshotsAndPathMappings() {
    UpgradeRecordEntity record = recordEntity();
    records.create(record);
    assertThat(records.find(record.getId()).getStatus()).isEqualTo(UpgradeStatus.STARTING);
    assertThat(records.find(record.getId()).getPackagePath()).isEqualTo("upload-id/package.zip");
    assertThat(records.running().getId()).isEqualTo(record.getId());

    List<ReleaseMapping> mappings = List.of(new ReleaseMapping("console",
            ReleaseMapping.Mode.SYMLINK, "console", temp.resolve("console").toString()));
    pathMappings.replace(record.getId(), mappings);
    assertThat(pathMappings.find(record.getId())).isEqualTo(mappings);
    assertThat(entityManager.find(UpgradePathMappingEntity.class))
            .singleElement().extracting(UpgradePathMappingEntity::getTarget)
            .isEqualTo(temp.getRoot().relativize(temp.resolve("console")).toString());

    records.status(record.getId(), UpgradeStatus.INSTALLING, "准备新版本");
    assertThat(records.find(record.getId()).getStatus()).isEqualTo(UpgradeStatus.INSTALLING);

    records.failed(record.getId(), "失败");
    assertThat(records.find(record.getId()).getStatus()).isEqualTo(UpgradeStatus.FAILED);
    assertThat(records.find(record.getId()).getFinishedAt()).isNotNull();
    assertThat(records.running()).isNull();
  }

  @Test
  void settingsAreStoredInDatabase() {
    settings.save(new UpgradeOptions(false, 120, 30, "数据库设置"));
    assertThat(settings.get().backupEnabled()).isFalse();
    assertThat(settings.get().backupTimeoutSeconds()).isEqualTo(120);
    assertThat(settings.get().healthTimeoutSeconds()).isEqualTo(30);
    assertThat(settings.get().note()).isEqualTo("数据库设置");
  }

  private UpgradePackageEntity packageEntity(String location) {
    UpgradePackageEntity entity = new UpgradePackageEntity();
    entity.setOriginalName("package.zip");
    entity.setDisplayName("测试包");
    entity.setStatus(UpgradePackageStatus.READY);
    entity.setVersion("3.5");
    entity.setBuild(10);
    entity.setFileSize(3);
    entity.setSha256("a".repeat(64));
    entity.setEntryCount(4);
    entity.setLocation(location);
    entity.setCreatedAt(Instant.parse("2026-11-14T00:00:00Z"));
    return entity;
  }

  private UpgradeRecordEntity recordEntity() {
    UpgradeRecordEntity entity = new UpgradeRecordEntity();
    entity.setPackageId(10L);
    entity.setPackageName("测试包");
    entity.setPackageVersion("3.5");
    entity.setPackageBuild(10);
    entity.setPackageSha256("a".repeat(64));
    entity.setPackagePath("upload-id/package.zip");
    entity.setStatus(UpgradeStatus.STARTING);
    entity.setBackupEnabled(true);
    entity.setBackupTimeoutSeconds(600);
    entity.setHealthTimeoutSeconds(90);
    entity.setCreatedAt(Instant.parse("2026-11-14T00:00:00Z"));
    return entity;
  }

}
