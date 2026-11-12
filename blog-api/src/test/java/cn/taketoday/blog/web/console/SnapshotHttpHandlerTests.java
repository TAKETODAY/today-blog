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

package cn.taketoday.blog.web.console;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import cn.taketoday.blog.model.AppOperation;
import cn.taketoday.blog.model.AppSnapshot;
import cn.taketoday.blog.service.SnapshotService;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.core.ParameterizedTypeReference;
import infra.persistence.EntityManager;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 快照管理接口的响应契约与 metadata 隔离测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
@WebAPITest
@Transactional
class SnapshotHttpHandlerTests {

  @Autowired
  private MockMvcTester mvc;

  @Autowired
  private EntityManager entityManager;

  @Test
  void listAndDetailReturnTypedContractWithoutMetadata() throws Exception {
    String id = UUID.randomUUID().toString();
    Properties metadata = new Properties();
    metadata.setProperty("scope", "程序、数据库、本地附件、配置");
    metadata.setProperty("password", "must-not-leak");
    StringWriter metadataText = new StringWriter();
    metadata.store(metadataText, null);
    AppSnapshot inserted = new AppSnapshot();
    inserted.setId(id);
    inserted.setName("升级前保护点");
    inserted.setStatus("AVAILABLE");
    inserted.setVersion("3.4");
    inserted.setBuild("123");
    inserted.setCreatedAt("2026-11-12T00:00:00Z");
    inserted.setProtectedSnapshot(true);
    inserted.setTotalBytes(1048576L);
    inserted.setMetadata(metadataText.toString());
    entityManager.persist(inserted);

    mvc.get().uri("/api/console/snapshots")
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(new ParameterizedTypeReference<List<SnapshotService.SnapshotView>>() { })
            .satisfies(snapshots -> assertThat(snapshots).anySatisfy(snapshot -> {
              assertThat(snapshot.id()).isEqualTo(id);
              assertThat(snapshot.name()).isEqualTo("升级前保护点");
              assertThat(snapshot.protectedSnapshot()).isTrue();
              assertThat(snapshot.totalBytes()).isEqualTo(1048576L);
              assertThat(snapshot.version()).isEqualTo("3.4");
            }));

    mvc.get().uri("/api/console/snapshots/{id}", id)
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(SnapshotService.SnapshotDetail.class)
            .satisfies(detail -> {
              assertThat(detail.snapshot().id()).isEqualTo(id);
              assertThat(detail.snapshot().protectedSnapshot()).isTrue();
              assertThat(detail.scope()).isEqualTo("程序、数据库、本地附件、配置");
              assertThat(detail.restoreVerifiedAt()).isNull();
              assertThat(detail.toString()).doesNotContain("must-not-leak");
            });
  }

  @Test
  void operationsIncludeSnapshotRecordsAndRelatedUpgradesOnly() throws Exception {
    String created = UUID.randomUUID().toString();
    String related = UUID.randomUUID().toString();
    String protectedBy = UUID.randomUUID().toString();
    String unrelated = UUID.randomUUID().toString();
    insertOperation(created, "SNAPSHOT_CREATE", null, null, "2026-11-12T00:00:02Z");
    insertOperation(related, "UPGRADE", "9c1cbb8e-2e5a-4b8a-8b2d-1f0b0d1a1001", null, "2026-11-12T00:00:03Z");
    insertOperation(protectedBy, "UPGRADE", null, "9c1cbb8e-2e5a-4b8a-8b2d-1f0b0d1a1002", "2026-11-12T00:00:01Z");
    insertOperation(unrelated, "UPGRADE", null, null, "2026-11-12T00:00:04Z");

    mvc.get().uri("/api/console/snapshots/operations")
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(new ParameterizedTypeReference<List<SnapshotService.OperationView>>() { })
            .satisfies(operations -> {
              assertThat(operations).extracting(SnapshotService.OperationView::id)
                      .contains(created, related, protectedBy)
                      .doesNotContain(unrelated);
              assertThat(operations).extracting(SnapshotService.OperationView::id)
                      .containsSubsequence(related, created, protectedBy);
            });
  }

  @Test
  void listsSnapshotsNewestFirst() throws Exception {
    String older = UUID.randomUUID().toString();
    String newer = UUID.randomUUID().toString();
    insertSnapshot(older, "旧快照", "2026-11-12T00:00:01Z");
    insertSnapshot(newer, "新快照", "2026-11-12T00:00:02Z");

    mvc.get().uri("/api/console/snapshots")
            .assertThat().hasStatusOk().bodyJson()
            .convertTo(new ParameterizedTypeReference<List<SnapshotService.SnapshotView>>() { })
            .satisfies(snapshots -> assertThat(snapshots)
                    .extracting(SnapshotService.SnapshotView::id)
                    .containsSubsequence(newer, older));
  }

  private void insertSnapshot(String id, String name, String createdAt) throws Exception {
    Properties metadata = new Properties();
    metadata.setProperty("id", id);
    StringWriter metadataText = new StringWriter();
    metadata.store(metadataText, null);
    AppSnapshot snapshot = new AppSnapshot();
    snapshot.setId(id);
    snapshot.setName(name);
    snapshot.setStatus("AVAILABLE");
    snapshot.setCreatedAt(createdAt);
    snapshot.setMetadata(metadataText.toString());
    entityManager.persist(snapshot);
  }

  private void insertOperation(String id, String type, String snapshotId, String protectionSnapshotId,
          String createdAt) throws Exception {
    Properties metadata = new Properties();
    metadata.setProperty("id", id);
    metadata.setProperty("status", "SUCCESS");
    metadata.setProperty("createdAt", createdAt);
    StringWriter metadataText = new StringWriter();
    metadata.store(metadataText, null);
    AppOperation operation = new AppOperation();
    operation.setId(id);
    operation.setOperationType(type);
    operation.setStatus("SUCCESS");
    operation.setCreatedAt(createdAt);
    operation.setSnapshotId(snapshotId);
    operation.setProtectionSnapshotId(protectionSnapshotId);
    operation.setMetadata(metadataText.toString());
    entityManager.persist(operation);
  }

}
