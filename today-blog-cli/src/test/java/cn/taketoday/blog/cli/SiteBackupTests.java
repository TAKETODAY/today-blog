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

package cn.taketoday.blog.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.taketoday.blog.updater.BackupType;
import cn.taketoday.blog.updater.UpgradeFiles;
import cn.taketoday.blog.updater.UpgradePackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证整站备份采集顺序及失败行为。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
@EnabledOnOs(OS.LINUX)
class SiteBackupTests {

  @TempDir
  Path temporary;

  private final UpgradeFiles files = new UpgradeFiles();

  private final UpgradePackage packages = new UpgradePackage();

  private final Properties settings = new Properties();

  private final FakeService service = new FakeService();

  private Path root;

  private Path output;

  private Path config;

  @BeforeEach
  void prepare() throws Exception {
    Path temp = temporary.toRealPath();
    root = Files.createDirectory(temp.resolve("install"));
    Path release = Files.createDirectories(root.resolve("releases/current-build"));
    Path zip = releaseZip(temp);
    packages.extract(zip, release, packages.verify(zip));
    Files.writeString(release.resolve("release.properties"), "version=4.0\nbuild=123\n");
    Files.createSymbolicLink(root.resolve("current"), release);
    output = Files.createDirectory(temp.resolve("backups"));
    config = temp.resolve("site-backup.properties");
    Path upload = Files.createDirectory(temp.resolve("upload"));
    Files.writeString(upload.resolve("头像.png"), "site file");
    settings.setProperty("output", output.toString());
    settings.setProperty("releaseZip", zip.toString());
    settings.setProperty("cliJar", Files.writeString(temp.resolve("cli.jar"), "cli").toString());
    settings.setProperty("installer", Files.writeString(temp.resolve("install.sh"), "installer").toString());
    settings.setProperty("tar", Path.of("/usr/bin/tar").toRealPath().toString());
    // 替身仅测试流程与归档内容；安全性由部署的 age 工具提供。
    settings.setProperty("age", executable("test-age", """
            if [ "$#" = 5 ]; then
              : > "$4"
            else
              /bin/cat
            fi
            """).toString());
    settings.setProperty("recipient", "age1" + "q".repeat(58));
    settings.setProperty("noExternalWriters", "true");
    settings.setProperty("source.upload", upload.toString());
    settings.setProperty("source.app-config", Files.writeString(temp.resolve("app.env"), "app config").toString());
    settings.setProperty("source.proxy-config", Files.writeString(temp.resolve("proxy.conf"), "proxy config").toString());
  }

  @Test
  void restoresArchivedInputsFromOneCapture() throws Exception {
    // 服务重启后的同名文件变更不进入本次归档。
    service.afterRestart = () -> Files.writeString(Path.of(settings.getProperty("source.upload")).resolve("头像.png"),
            "next version of the file");
    Path archive = create(this::dump);
    assertThat(service.running).isTrue();
    assertThat(Files.getPosixFilePermissions(archive)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
    assertThat(output.toFile().list()).containsExactly(archive.getFileName().toString());
    Path site = extract(archive);
    assertThat(Files.readString(site.resolve("files/upload/头像.png"))).isEqualTo("site file");
    assertThat(Files.readString(site.resolve("files/app-config"))).isEqualTo("app config");
    assertThat(site.resolve("oss")).doesNotExist();
    assertThat(packages.verify(site.resolve("artifacts/release.zip")).build()).isEqualTo(123);
    assertThat(files.read(site.resolve("manifest.properties"))).containsEntry("build", "123")
            .containsEntry("oss", "none").containsEntry("siteScope", "full-local-site")
            .containsEntry("types", BackupType.encode(BackupType.recommended()));
    assertThat(Files.readString(site.resolve("SHA256SUMS"))).contains("  database.sql.gz", "  files/upload/头像.png");
    try (var sql = new GZIPInputStream(Files.newInputStream(site.resolve("database.sql.gz")))) {
      assertThat(new String(sql.readAllBytes(), StandardCharsets.UTF_8)).contains("CREATE DATABASE");
    }
  }

  @Test
  void databaseOnlyDoesNotNeedFileSourcesOrStopTheService() throws Exception {
    settings.remove("source.upload");
    settings.remove("releaseZip");
    settings.remove("noExternalWriters");
    files.write(config, settings);
    Path site = extract(executor(destination -> {
      assertThat(service.running).isTrue();
      writeDump(destination);
    }).create(config, Set.of(BackupType.DATABASE), false));
    assertThat(service.stops).isZero();
    assertThat(service.restarts).isZero();
    assertThat(site.resolve("database.sql.gz")).exists();
    assertThat(site.resolve("files")).doesNotExist();
    assertThat(site.resolve("artifacts")).doesNotExist();
    assertThat(files.read(site.resolve("manifest.properties"))).containsEntry("consistency", "online-database")
            .containsEntry("siteScope", "partial").containsEntry("types", "DATABASE");
  }

  @Test
  void capturesOnlySelectedFileProviders() throws Exception {
    settings.setProperty("source.website-theme", Files.writeString(temporary.resolve("theme"), "theme").toString());
    files.write(config, settings);
    Path site = extract(executor(destination -> {
      throw new AssertionError("Unselected database provider must not run");
    }).create(config, Set.of(BackupType.ATTACHMENTS, BackupType.WEBSITE), false));
    assertThat(service.stops).isEqualTo(1);
    assertThat(service.restarts).isEqualTo(1);
    assertThat(site.resolve("files/upload/头像.png")).exists();
    assertThat(site.resolve("files/website-theme")).hasContent("theme");
    assertThat(site.resolve("files/app-config")).doesNotExist();
    assertThat(site.resolve("database.sql.gz")).doesNotExist();
    assertThat(files.read(site.resolve("manifest.properties"))).containsEntry("siteScope", "partial")
            .containsEntry("types", "ATTACHMENTS,WEBSITE");
  }

  @Test
  void includesConfiguredOssOnlyWhenSelected() throws Exception {
    settings.setProperty("ossExporter", executable("test-oss", """
            echo '[{"key":"avatar.png"}]' > "$1/inventory.json"
            echo object > "$1/object"
            """).toString());
    files.write(config, settings);
    Path site = extract(executor(this::dump).create(config, Set.of(BackupType.OSS), false));
    assertThat(site.resolve("oss/object")).hasContent("object");
    assertThat(site.resolve("database.sql.gz")).doesNotExist();
    assertThat(files.read(site.resolve("manifest.properties"))).containsEntry("oss", "export")
            .containsEntry("types", "OSS");
    assertThat(Files.readString(site.resolve("SHA256SUMS"))).contains("  oss/inventory.json", "  oss/object");
  }

  @Test
  void resumesServiceWithoutPublishingWhenDatabaseFails() throws Exception {
    assertThatThrownBy(() -> create(destination -> {
      assertThat(service.running).isFalse();
      throw new IOException("dump failed");
    })).hasMessage("dump failed");
    assertThat(service.restarts).isEqualTo(1);
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void refusesConcurrentUpgrade() throws Exception {
    try (var channel = FileChannel.open(root.resolve("upgrade.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.lock()) {
      assertThatThrownBy(() -> create(this::dump)).hasMessageContaining("已有升级任务");
    }
    assertThat(service.stops).isZero();
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void rejectsChangedReleaseBeforeStoppingService() throws Exception {
    Files.writeString(root.resolve("current/desktop/index.html"), "modified");
    assertThatThrownBy(() -> create(this::dump)).hasMessageContaining("发行文件与备份用发行 ZIP 不匹配");
    assertThat(service.stops).isZero();
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void rejectsFailedTarEvenWhenEncryptionSucceeds() throws Exception {
    settings.setProperty("tar", executable("broken-tar", "echo partial\nexit 1\n").toString());
    assertThatThrownBy(() -> create(this::dump)).hasMessageContaining("压缩或加密失败");
    assertThat(service.restarts).isEqualTo(1);
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void doesNotPublishWhenRestartFails() throws Exception {
    service.afterRestart = () -> {
      throw new IOException("restart failed");
    };
    assertThatThrownBy(() -> create(this::dump)).hasMessageContaining("备份后 API 未恢复");
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void doesNotFollowSymlinksInsideWebsiteFiles() throws Exception {
    Path upload = Path.of(settings.getProperty("source.upload"));
    Files.createSymbolicLink(upload.resolve("external"), config);
    assertThatThrownBy(() -> create(this::dump)).hasMessageContaining("符号链接");
    assertThat(service.restarts).isEqualTo(1);
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void rejectsUnconfiguredOssBeforeStoppingService() throws Exception {
    files.write(config, settings);
    assertThatThrownBy(() -> executor(this::dump).create(config,
            Set.of(BackupType.OSS), false)).hasMessageContaining("ossExporter");
    assertThat(service.stops).isZero();
    assertThat(service.restarts).isZero();
    assertThat(output.toFile().list()).isEmpty();
  }

  @Test
  void leavesLocalArchiveWhenOffsiteUploadFailsAndKeepsStoppedServiceStopped() throws Exception {
    service.running = false;
    settings.setProperty("offsiteUploader", executable("failed-upload", "exit 9\n").toString());
    assertThatThrownBy(() -> create(this::dump)).isInstanceOfSatisfying(SiteBackup.OffsiteUploadFailed.class,
            failure -> assertThat(failure.archive()).exists());
    assertThat(service.stops).isZero();
    assertThat(service.restarts).isZero();
    assertThat(output.toFile().list()).hasSize(1).allMatch(name -> name.endsWith(".tar.gz.age"));
  }

  private Path create(SiteBackup.Database.Dump dump) throws Exception {
    files.write(config, settings);
    return executor(dump).create(config);
  }

  private SiteBackup executor(SiteBackup.Database.Dump dump) {
    return new SiteBackup(files, packages, root, service, staging -> dump, (path, privatePath) -> {
      assertThat(path).exists();
    });
  }

  private void dump(Path destination) throws IOException {
    assertThat(service.running).isFalse();
    writeDump(destination);
  }

  private static void writeDump(Path destination) throws IOException {
    try (var zip = new GZIPOutputStream(Files.newOutputStream(destination))) {
      zip.write("CREATE DATABASE IF NOT EXISTS today;\nUSE today;\n".getBytes(StandardCharsets.UTF_8));
    }
  }

  private Path extract(Path archive) throws Exception {
    Path restored = Files.createDirectory(temporary.resolve("restore"));
    Process tar = new ProcessBuilder(settings.getProperty("tar"), "-xzf", archive.toString(), "-C", restored.toString())
            .inheritIO().start();
    assertThat(tar.waitFor()).isZero();
    return restored.resolve("site");
  }

  private Path executable(String name, String content) throws IOException {
    Path path = Files.writeString(temporary.resolve(name), "#!/bin/sh\nset -eu\n" + content);
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
    return path;
  }

  private static Path releaseZip(Path temp) throws Exception {
    Map<String, String> contents = Map.of("api/bin/blog-api", "api", "api/lib/blog.jar", "jar",
            "console/index.html", "console", "desktop/index.html", "desktop");
    Path archive = temp.resolve("release.zip");
    try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
      StringBuilder manifest = new StringBuilder("product=today-blog\nformat=1\nversion=4.0\nbuild=123\n");
      for (var entry : contents.entrySet()) {
        byte[] body = entry.getValue().getBytes(StandardCharsets.UTF_8);
        manifest.append("sha256.").append(entry.getKey()).append('=').append(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(body))).append('\n');
        zip.putNextEntry(new ZipEntry("today-blog/" + entry.getKey()));
        zip.write(body);
        zip.closeEntry();
      }
      zip.putNextEntry(new ZipEntry("today-blog/META-INF/upgrade.properties"));
      zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return archive;
  }

  private static class FakeService implements SiteBackup.Service {

    boolean running = true;

    int stops;

    int restarts;

    AfterRestart afterRestart;

    @Override
    public boolean running() {
      return running;
    }

    @Override
    public void stop() {
      stops++;
      running = false;
    }

    @Override
    public void restartAndWait(int timeoutSeconds) throws Exception {
      restarts++;
      running = true;
      if (afterRestart != null) {
        afterRestart.run();
      }
    }
  }

  private interface AfterRestart {
    void run() throws Exception;
  }
}
