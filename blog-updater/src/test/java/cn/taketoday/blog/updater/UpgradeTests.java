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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeTests {

  @TempDir
  Path temp;

  @Test
  void backupRunsBeforeSwitchAndIsRecorded() throws Exception {
    Scenario scenario = scenario(false);
    FakeStore store = new FakeStore();
    FakeService service = new FakeService(false, false);
    BlogUpdaterApplication.run(scenario.root, scenario.task, service, store, (id, timeout) -> {
      assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("old");
      assertThat(service.restarts).isZero();
      assertThat(timeout).isEqualTo(600);
      return backup(scenario, "backup-id");
    });
    assertThat(store.backup).isNotNull();
    assertThat(store.backup.id()).isEqualTo("backup-id");
    assertThat(store.statuses).contains("BACKING_UP", "SUCCESS");
  }

  @Test
  void backupFailureNeverSwitchesOrRestarts() throws Exception {
    Scenario scenario = scenario(false);
    FakeStore store = new FakeStore();
    FakeService service = new FakeService(false, false);
    assertThatThrownBy(() -> BlogUpdaterApplication.run(scenario.root, scenario.task, service, store,
            (id, timeout) -> {
              throw new java.io.IOException("sensitive detail");
            })).hasMessageContaining("未切换版本").hasMessageNotContaining("sensitive");
    assertThat(service.restarts).isZero();
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("old");
    assertThat(store.statuses).contains("FAILED");
    assertThat(store.message).doesNotContain("sensitive");
  }

  @Test
  void acceptsPackageWithoutSignatureAndExtracts() throws Exception {
    Path archive = archive(null, false, false);
    var manifest = UpgradePackage.verify(archive);
    Path destination = Files.createDirectory(temp.resolve("extracted"));
    UpgradePackage.extract(archive, destination, manifest);
    assertThat(manifest.version()).isEqualTo("3.4");
    assertThat(Files.readString(destination.resolve("console/index.html"))).isEqualTo("console");
  }

  @Test
  void rejectsMissingManifest() throws Exception {
    assertThatThrownBy(() -> UpgradePackage.verify(archive(null, true, false))).hasMessageContaining("缺少清单");
  }

  @Test
  void rejectsTamperedFile() throws Exception {
    assertThatThrownBy(() -> UpgradePackage.verify(archive(null, false, true))).hasMessageContaining("摘要不匹配");
  }

  @Test
  void rejectsUnlistedFileAndTraversal() throws Exception {
    assertThatThrownBy(() -> UpgradePackage.verify(archive("console/extra.js", false, false)))
            .hasMessageContaining("未列入清单");
    assertThatThrownBy(() -> UpgradePackage.verify(archive("../outside", false, false)))
            .hasMessageContaining("非法路径");
  }

  @Test
  void installsAndVerifiesNewRelease() throws Exception {
    Scenario scenario = scenario(false);
    FakeStore store = new FakeStore();
    FakeService service = new FakeService(false, false);
    BlogUpdaterApplication.run(scenario.root, scenario.task, service, store, (id, timeout) -> backup(scenario, id));
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("3.4-2");
    assertThat(store.statuses.get(store.statuses.size() - 1)).isEqualTo("SUCCESS");
    assertThat(service.restarts).isEqualTo(1);
    assertThat(UpgradeFiles.read(scenario.root.resolve("highest-build.properties")).getProperty("build")).isEqualTo("2");
  }

  @Test
  void failedHealthCheckRestoresOldRelease() throws Exception {
    Scenario scenario = scenario(false);
    FakeStore store = new FakeStore();
    FakeService service = new FakeService(true, false);
    BlogUpdaterApplication.run(scenario.root, scenario.task, service, store, (id, timeout) -> backup(scenario, id));
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("old");
    assertThat(store.statuses).contains("ROLLED_BACK");
    assertThat(service.restarts).isEqualTo(2);
  }

  @Test
  void failedRollbackIsReportedAsFailure() throws Exception {
    Scenario scenario = scenario(false);
    FakeStore store = new FakeStore();
    assertThatThrownBy(() -> BlogUpdaterApplication.run(scenario.root, scenario.task,
            new FakeService(true, true), store, (id, timeout) -> backup(scenario, id)))
            .hasMessageContaining("健康检查失败");
    assertThat(store.statuses.get(store.statuses.size() - 1)).isEqualTo("FAILED");
  }

  @Test
  void rejectsPreviouslySuccessfulBuildWithoutRestart() throws Exception {
    Scenario scenario = scenario(false);
    FakeStore store = new FakeStore();
    Properties highest = new Properties();
    highest.setProperty("build", "2");
    UpgradeFiles.write(scenario.root.resolve("highest-build.properties"), highest);
    FakeService service = new FakeService(false, false);
    assertThatThrownBy(() -> BlogUpdaterApplication.run(scenario.root, scenario.task, service, store,
            (id, timeout) -> backup(scenario, id))).hasMessageContaining("降级");
    assertThat(service.restarts).isZero();
    assertThat(store.statuses).contains("FAILED");
  }

  @Test
  void deletesPackageAfterSuccessWhenRequested() throws Exception {
    Scenario scenario = scenario(true);
    FakeStore store = new FakeStore();
    BlogUpdaterApplication.run(scenario.root, scenario.task,
            new FakeService(false, false), store, (id, timeout) -> backup(scenario, id));
    assertThat(store.deletedPackage).isEqualTo("package-1");
    assertThat(Files.exists(Path.of(scenario.task.packagePath()))).isFalse();
  }

  @Test
  void rejectsInvalidTaskId() {
    assertThatThrownBy(() -> UpgradeFiles.checkId("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
  }

  private Scenario scenario(boolean deletePackage) throws Exception {
    Path root = Files.createDirectory(temp.resolve("install"));
    Path old = Files.createDirectories(root.resolve("releases/old/console"));
    Files.writeString(old.resolve("index.html"), "old");
    Properties metadata = new Properties();
    metadata.setProperty("version", "3.3");
    metadata.setProperty("build", "1");
    UpgradeFiles.write(old.getParent().resolve("release.properties"), metadata);
    Files.createSymbolicLink(root.resolve("current"), old.getParent());
    String id = UUID.randomUUID().toString();
    UpgradeTask task = new UpgradeTask(id, "package-1", archive(null, false, false).toString(),
            "3.4", 2, 1L, true, 600, 90, deletePackage, List.of());
    return new Scenario(root, task);
  }

  private DatabaseBackupResult backup(Scenario scenario, String id) {
    return new DatabaseBackupResult(id, temp.resolve("backup.sql.gz"), 1024,
            "a".repeat(64), Instant.parse("2026-11-14T00:00:00Z"));
  }

  private Path archive(String extra, boolean missingManifest, boolean corruptFile) throws Exception {
    java.util.Map<String, String> files = new LinkedHashMap<>();
    files.put("api/bin/blog-api", "#!/bin/sh\n");
    files.put("api/lib/app.jar", "jar");
    files.put("console/index.html", "console");
    files.put("desktop/index.html", "desktop");
    StringBuilder metadata = new StringBuilder("format=1\nproduct=today-blog\nversion=3.4\nbuild=2\nminimumJava=17\n");
    for (var file : files.entrySet()) {
      metadata.append("sha256.").append(file.getKey()).append('=')
              .append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                      .digest(file.getValue().getBytes(StandardCharsets.UTF_8))))
              .append('\n');
    }
    if (!missingManifest)
      files.put("META-INF/upgrade.properties", metadata.toString());
    if (corruptFile)
      files.put("api/lib/app.jar", "modified");
    if (extra != null)
      files.put(extra, "unexpected");
    Path archive = Files.createTempFile(temp, "package-", ".zip");
    try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
      for (var file : files.entrySet()) {
        output.putNextEntry(new ZipEntry("today-blog/" + file.getKey()));
        output.write(file.getValue().getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
      }
    }
    return archive;
  }

  private record Scenario(Path root, UpgradeTask task) {
  }

  static class FakeStore implements UpgradeTaskStore {
    final List<String> statuses = new ArrayList<>();
    DatabaseBackupResult backup;
    String deletedPackage;
    String message;

    @Override
    public UpgradeTask load(String id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void status(String id, String status, String message) {
      statuses.add(status);
      this.message = message;
    }

    @Override
    public void started(String id, String releasePath) {
      statuses.add("STARTED");
    }

    @Override
    public void backup(String id, DatabaseBackupResult backup) {
      this.backup = backup;
    }

    @Override
    public void finished(String id, String status, String message) {
      statuses.add(status);
      this.message = message;
    }

    @Override
    public void deletePackage(String packageId) {
      deletedPackage = packageId;
    }

    @Override
    public void close() {
    }
  }

  private static class FakeService implements BlogUpdaterApplication.ServiceControl {
    final boolean failNew;
    final boolean failOld;
    int restarts;

    FakeService(boolean failNew, boolean failOld) {
      this.failNew = failNew;
      this.failOld = failOld;
    }

    @Override
    public void restart() {
      restarts++;
    }

    @Override
    public boolean healthy(long build) {
      return build == 2 ? !failNew : !failOld;
    }
  }

}
