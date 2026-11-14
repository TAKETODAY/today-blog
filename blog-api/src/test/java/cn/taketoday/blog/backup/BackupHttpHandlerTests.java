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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.core.ParameterizedTypeReference;
import infra.persistence.EntityManager;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Test
  void backupRoutesRemainAvailableAndWritesStayDisabled() {
    String base = "/api/console/backups";
    mvc.get().uri(base).assertThat().hasStatusOk();
    mvc.get().uri(base + "/capabilities").assertThat().hasStatusOk();
    mvc.post().uri(base).assertThat().hasStatus(503);
    String id = UUID.randomUUID().toString();
    mvc.post().uri(base + "/{id}/restore", id).assertThat().hasStatus(503);
    mvc.put().uri(base + "/{id}/protection", id).assertThat().hasStatus(503);
    mvc.delete().uri(base + "/{id}", id).assertThat().hasStatus(503);
  }

  @Test
  void legacyRoutesAreNotAvailable() {
    String base = "/api/console/snapshots";
    for (String suffix : List.of("", "/capabilities", "/" + UUID.randomUUID())) {
      mvc.get().uri(base + suffix).assertThat().hasStatus(404);
    }
    String id = UUID.randomUUID().toString();
    mvc.post().uri(base).assertThat().hasStatus(404);
    mvc.post().uri(base + "/{id}/restore", id).assertThat().hasStatus(404);
    mvc.put().uri(base + "/{id}/protection", id).assertThat().hasStatus(404);
    mvc.delete().uri(base + "/{id}", id).assertThat().hasStatus(404);
  }

  @Test
  void listAndDetailReturnTypedContractWithoutMetadata() throws Exception {
    String id = UUID.randomUUID().toString();
    Backup inserted = new Backup();
    inserted.setId(id);
    inserted.setName("升级前保护点");
    inserted.setStatus(BackupStatus.AVAILABLE);
    inserted.setVersion("3.4");
    inserted.setBuild("123");
    inserted.setCreatedAt("2026-11-12T00:00:00Z");
    inserted.setProtectedBackup(true);
    inserted.setTotalBytes(1048576L);
    inserted.setLocation("/var/backups/today-blog/" + id + "/database.sql.gz");
    inserted.setScope("程序、数据库、本地附件、配置");
    entityManager.persist(inserted);

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
              assertThat(detail.location()).endsWith("/database.sql.gz");
            });
  }

  @Test
  void listsBackupsNewestFirst() throws Exception {
    String older = UUID.randomUUID().toString();
    String newer = UUID.randomUUID().toString();
    insertBackup(older, "旧备份", "2026-11-12T00:00:01Z");
    insertBackup(newer, "新备份", "2026-11-12T00:00:02Z");

    mvc.get().uri("/api/console/backups")
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(new ParameterizedTypeReference<List<BackupService.BackupView>>() {
            })
            .satisfies(backups -> assertThat(backups)
                    .extracting(BackupService.BackupView::id)
                    .containsSubsequence(newer, older));
  }

  private void insertBackup(String id, String name, String createdAt) {
    Backup backup = new Backup();
    backup.setId(id);
    backup.setName(name);
    backup.setStatus(BackupStatus.AVAILABLE);
    backup.setCreatedAt(createdAt);
    entityManager.persist(backup);
  }

}
