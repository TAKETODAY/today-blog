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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.updater.DatabaseBackupFactory;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.persistence.EntityManager;
import infra.session.HeaderSessionIdResolver;
import infra.session.SessionRepository;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * 系统备份接口的响应契约测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@WebAPITest
@Transactional
class BackupHttpHandlerTests {

  @Autowired
  private MockMvcTester mvc;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private SessionRepository sessions;

  @MockitoBean
  private DatabaseBackupFactory archives;

  @TempDir
  Path directory;

  @Test
  void bloggerCanCheckAndDownloadExactArchive() throws IOException {
    Backup backup = insertDatabaseBackup();
    Path archive = directory.resolve("database.sql.gz");
    try (var gzip = new GZIPOutputStream(Files.newOutputStream(archive))) {
      gzip.write(new byte[] { 1, 2, 3 });
    }
    byte[] bytes = Files.readAllBytes(archive);
    when(archives.archive(backup.getLocation())).thenReturn(archive);

    String route = "/api/console/backups/{id}/download";
    mvc.get().uri(route, backup.getId()).assertThat().hasStatusOk()
            .hasHeader(HttpHeaders.CONTENT_TYPE, "application/gzip")
            .hasHeader(HttpHeaders.CACHE_CONTROL, "no-store")
            .hasHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"database-backup-" + backup.getId() + ".sql.gz\"")
            .body().isEqualTo(bytes);
    mvc.method(HttpMethod.HEAD).uri(route, backup.getId()).assertThat().hasStatusOk()
            .hasHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(bytes.length))
            .body().isEmpty();
  }

  @Test
  void rejectsDownloadsWithoutBloggerIdentity() {
    Backup backup = insertDatabaseBackup();
    String route = "/api/console/backups/{id}/download";
    mvc.get().uri(route, backup.getId())
            .with((request, context) -> request.removeHeader(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO))
            .assertThat().hasStatus(401);

    Blogger.unbind(sessions.retrieveSession("backup-download-reader"));
    mvc.get().uri(route, backup.getId())
            .header(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO, "backup-download-reader")
            .assertThat().hasStatus(404);
  }

  @Test
  void missingArchiveAndUnfinishedBackupCannotBeDownloaded() throws IOException {
    Backup backup = insertDatabaseBackup();
    when(archives.archive(backup.getLocation())).thenThrow(new IOException("unreadable"));
    String route = "/api/console/backups/{id}/download";
    mvc.get().uri(route, Long.MAX_VALUE).assertThat().hasStatus(404);
    mvc.get().uri(route, backup.getId()).assertThat().hasStatus(404);

    backup.setStatus(BackupStatus.CREATING);
    entityManager.updateById(backup);
    Path archive = Files.createFile(directory.resolve("unfinished.sql.gz"));
    doReturn(archive).when(archives).archive(backup.getLocation());
    mvc.get().uri(route, backup.getId()).assertThat().hasStatus(404);
  }

  @Test
  void backupRoutesRemainAvailableAndWritesStayDisabled() {
    String base = "/api/console/backups";
    mvc.get().uri(base).assertThat().hasStatusOk();
    mvc.get().uri(base + "/capabilities").assertThat().hasStatusOk();
    mvc.get().uri(base + "/jobs").assertThat().hasStatusOk();
    mvc.post().uri(base).assertThat().hasStatus(503);
    String id = "1";
    mvc.post().uri(base + "/{id}/restore", id).assertThat().hasStatus(503);
    mvc.put().uri(base + "/{id}/protection", id).assertThat().hasStatus(503);
    mvc.delete().uri(base + "/{id}", id).assertThat().hasStatus(503);
  }

  @Test
  void backupJobsRequireAuthentication() {
    String base = "/api/console/backups";
    mvc.get().uri(base + "/jobs")
            .with((request, context) -> request.removeHeader(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO))
            .assertThat().hasStatus(401);
    mvc.post().uri(base)
            .with((request, context) -> request.removeHeader(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO))
            .assertThat().hasStatus(401);
  }

  @Test
  void legacyRoutesAreNotAvailable() {
    String base = "/api/console/snapshots";
    for (String suffix : List.of("", "/capabilities", "/" + UUID.randomUUID())) {
      mvc.get().uri(base + suffix).assertThat().hasStatus(404);
    }
    String id = "1";
    mvc.post().uri(base).assertThat().hasStatus(404);
    mvc.post().uri(base + "/{id}/restore", id).assertThat().hasStatus(404);
    mvc.put().uri(base + "/{id}/protection", id).assertThat().hasStatus(404);
    mvc.delete().uri(base + "/{id}", id).assertThat().hasStatus(404);
  }

  @Test
  void listAndDetailReturnTypedContractWithoutMetadata() throws Exception {
    Backup inserted = new Backup();
    inserted.setName("升级前保护点");
    inserted.setStatus(BackupStatus.AVAILABLE);
    inserted.setVersion("3.4");
    inserted.setBuild("123");
    inserted.setCreatedAt(Instant.parse("2026-11-12T00:00:00Z"));
    inserted.setProtectedBackup(true);
    inserted.setTotalBytes(1048576L);
    inserted.setScope("程序、数据库、本地附件、配置");
    entityManager.persist(inserted);
    Long id = inserted.getId();
    inserted.setLocation(id + "/database.sql.gz");
    entityManager.updateById(inserted);

    mvc.get().uri("/api/console/backups")
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(new ParameterizedTypeReference<List<BackupService.BackupView>>() {
            })
            .satisfies(backups -> assertThat(backups).anySatisfy(backup -> {
              assertThat(backup.id()).isEqualTo(id);
              assertThat(backup.name()).isEqualTo("升级前保护点");
              assertThat(backup.protectedBackup()).isTrue();
              assertThat(backup.totalBytes()).isEqualTo(1048576L);
              assertThat(backup.version()).isEqualTo("3.4");
            }));

    mvc.get().uri("/api/console/backups/{id}", id)
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(BackupService.BackupDetail.class)
            .satisfies(detail -> {
              assertThat(detail.backup().id()).isEqualTo(id);
              assertThat(detail.backup().protectedBackup()).isTrue();
              assertThat(detail.scope()).isEqualTo("程序、数据库、本地附件、配置");
              assertThat(detail.restoreVerifiedAt()).isNull();
              assertThat(detail.location()).isEqualTo(id + "/database.sql.gz");
            });
  }

  @Test
  void listsBackupsNewestFirst() throws Exception {
    Backup older = insertBackup("旧备份", "2026-11-12T00:00:01Z");
    Backup newer = insertBackup("新备份", "2026-11-12T00:00:02Z");

    mvc.get().uri("/api/console/backups")
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(new ParameterizedTypeReference<List<BackupService.BackupView>>() {
            })
            .satisfies(backups -> assertThat(backups)
                    .extracting(BackupService.BackupView::id)
                    .containsSubsequence(newer.getId(), older.getId()));
  }

  private Backup insertBackup(String name, String createdAt) {
    Backup backup = new Backup();
    backup.setName(name);
    backup.setStatus(BackupStatus.AVAILABLE);
    backup.setCreatedAt(Instant.parse(createdAt));
    entityManager.persist(backup);
    return backup;
  }

  private Backup insertDatabaseBackup() {
    Backup backup = insertBackup("升级前数据库备份", "2026-11-12T00:00:00Z");
    backup.setScope("数据库");
    backup.setLocation(backup.getId() + "/database.sql.gz");
    entityManager.updateById(backup);
    return backup;
  }

}
