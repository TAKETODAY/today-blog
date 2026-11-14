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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReleaseMappingTests {

  @TempDir
  Path temp;

  @Test
  void roundTripsTypedMappings() throws Exception {
    Path file = temp.resolve("mappings.conf");
    ReleaseMapping console = new ReleaseMapping("console", ReleaseMapping.Mode.SYMLINK,
            "console", temp.resolve("www/console").toString());
    ReleaseMapping desktop = new ReleaseMapping("desktop", ReleaseMapping.Mode.SYMLINK,
            "desktop", temp.resolve("www/desktop").toString());
    ReleaseMappings.write(file, List.of(console, desktop));

    assertThat(ReleaseMappings.read(file)).containsExactly(console, desktop);
  }

  @Test
  void rejectsUnknownFieldsAndInvalidValues() throws Exception {
    Path file = temp.resolve("mappings.conf");
    Files.writeString(file, "console\tsymlink\tconsole\t" + temp.resolve("www/console") + "\n", StandardCharsets.UTF_8);
    assertThat(ReleaseMappings.read(file)).hasSize(1);

    Files.writeString(file, "console\tsymlink\tapi/bin\t/var/www/console\n", StandardCharsets.UTF_8);
    assertThatThrownBy(() -> ReleaseMappings.read(file)).hasMessageContaining("映射源");

    Files.writeString(file, "console\tcopy\tconsole\t/var/www/console\n", StandardCharsets.UTF_8);
    assertThatThrownBy(() -> ReleaseMappings.read(file)).hasMessageContaining("映射方式");

    Files.writeString(file, "console\tsymlink\tconsole\n", StandardCharsets.UTF_8);
    assertThatThrownBy(() -> ReleaseMappings.read(file)).hasMessageContaining("四个");

    Files.writeString(file, "console\tsymlink\tconsole\t../console\n", StandardCharsets.UTF_8);
    assertThatThrownBy(() -> ReleaseMappings.read(file)).hasMessageContaining("绝对路径");

    Files.writeString(file, "console\tsymlink\tconsole/../etc\t/var/www/console\n", StandardCharsets.UTF_8);
    assertThatThrownBy(() -> ReleaseMappings.read(file)).hasMessageContaining("映射源");

    ReleaseMapping fileSource = new ReleaseMapping("entry", ReleaseMapping.Mode.SYMLINK,
            "console/index.html", temp.resolve("www/index.html").toString());
    assertThat(fileSource.source()).isEqualTo("console/index.html");
  }

  @Test
  void rejectsDuplicateAndNestedTargets() {
    Path www = temp.resolve("www");
    Path console = www.resolve("console");
    Path nested = console.resolve("assets");
    assertThatThrownBy(() -> ReleaseMappings.validate(List.of(
            mapping("console", console), mapping("console", www.resolve("other"))), List.of(www)))
            .hasMessageContaining("名称重复");
    ReleaseMapping assets = new ReleaseMapping("assets", ReleaseMapping.Mode.SYMLINK,
            "console/assets", nested.toString());
    assertThatThrownBy(() -> ReleaseMappings.validate(List.of(mapping("console", console), assets), List.of(www)))
            .hasMessageContaining("嵌套");
  }

  @Test
  void rejectsTargetOutsideAllowedRoots() {
    Path www = temp.resolve("www");
    Path console = www.resolve("console");
    assertThatThrownBy(() -> ReleaseMappings.validate(List.of(mapping("console", console)), List.of(temp.resolve("other"))))
            .hasMessageContaining("允许的根目录");
    ReleaseMappings.validate(List.of(mapping("console", console)), List.of(www.getParent()));
  }

  @Test
  void publishCreatesLinkThroughCurrent() throws Exception {
    Path install = Files.createDirectories(temp.resolve("install"));
    Path state = Files.createDirectories(temp.resolve("state"));
    Path www = Files.createDirectories(temp.resolve("www"));
    Files.createDirectories(install.resolve("releases/v1/console"));
    Files.createSymbolicLink(install.resolve("current"), install.resolve("releases/v1"));
    Path target = www.resolve("console");
    ReleaseMapping mapping = mapping("console", target);

    ReleaseMappings.publish(install, state, List.of(mapping), List.of(www));

    assertThat(Files.isSymbolicLink(target)).isTrue();
    assertThat(Files.readSymbolicLink(target)).isEqualTo(install.resolve("current/console"));
    assertThat(Files.isDirectory(target)).isTrue();

    // 重复发布保持幂等。
    ReleaseMappings.publish(install, state, List.of(mapping), List.of(www));
    assertThat(Files.readSymbolicLink(target)).isEqualTo(install.resolve("current/console"));
  }

  @Test
  void publishReplacesEmptyDirectoryButRefusesContent() throws Exception {
    Path install = Files.createDirectories(temp.resolve("install"));
    Path state = Files.createDirectories(temp.resolve("state"));
    Path www = Files.createDirectories(temp.resolve("www"));
    Files.createDirectories(install.resolve("releases/v1/console"));
    Files.createSymbolicLink(install.resolve("current"), install.resolve("releases/v1"));
    Path target = Files.createDirectories(www.resolve("console"));

    ReleaseMappings.publish(install, state, List.of(mapping("console", target)), List.of(www));
    assertThat(Files.isSymbolicLink(target)).isTrue();

    Path other = Files.createDirectories(www.resolve("desktop"));
    Files.writeString(other.resolve("index.html"), "manual");
    assertThatThrownBy(() -> ReleaseMappings.publish(install, state,
            List.of(mapping("desktop", other)), List.of(www)))
            .hasMessageContaining("已存在");
    assertThat(Files.isDirectory(other)).isTrue();
  }

  @Test
  void publishRefusesInstallAndStateDirectories() throws Exception {
    Path install = Files.createDirectories(temp.resolve("install"));
    Path state = Files.createDirectories(temp.resolve("state"));
    Path www = Files.createDirectories(temp.resolve("www"));
    Files.createDirectories(install.resolve("releases/v1/console"));
    Files.createSymbolicLink(install.resolve("current"), install.resolve("releases/v1"));

    assertThatThrownBy(() -> ReleaseMappings.publish(install, state,
            List.of(mapping("console", install.resolve("console"))), List.of(install.getParent())))
            .hasMessageContaining("不能与安装目录");
  }

  @Test
  void upgradePublishesMappingAndRollbackKeepsCurrentLink() throws Exception {
    Scenario scenario = scenario();
    Path www = Files.createDirectories(temp.resolve("www"));
    Path policy = Files.writeString(temp.resolve("mapping-roots"), www + "\n");
    Path target = www.resolve("console");
    Files.writeString(scenario.state.resolve("incoming").resolve(scenario.id).resolve("mappings.conf"),
            "console\tsymlink\tconsole\t" + target + "\n");

    BlogUpdaterApplication.run(scenario.root, scenario.state, scenario.id,
            new FakeService(false, false), policy);

    assertThat(Files.readSymbolicLink(target)).isEqualTo(scenario.root.resolve("current/console"));
    assertThat(Files.readString(target.resolve("index.html"))).isEqualTo("console");
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("3.4-2");
  }

  @Test
  void failedUpgradeLeavesMappingPointingAtRolledBackRelease() throws Exception {
    Scenario scenario = scenario();
    Files.createDirectories(scenario.root.resolve("releases/old/console"));
    Files.writeString(scenario.root.resolve("releases/old/console/index.html"), "old");
    Path www = Files.createDirectories(temp.resolve("www"));
    Path policy = Files.writeString(temp.resolve("mapping-roots"), www + "\n");
    Path target = www.resolve("console");
    Files.writeString(scenario.state.resolve("incoming").resolve(scenario.id).resolve("mappings.conf"),
            "console\tsymlink\tconsole\t" + target + "\n");

    BlogUpdaterApplication.run(scenario.root, scenario.state, scenario.id,
            new FakeService(true, false), policy);

    assertThat(Files.readSymbolicLink(target)).isEqualTo(scenario.root.resolve("current/console"));
    assertThat(Files.readString(target.resolve("index.html"))).isEqualTo("old");
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("old");
  }

  private ReleaseMapping mapping(String name, Path target) {
    return new ReleaseMapping(name, ReleaseMapping.Mode.SYMLINK, name, target.toString());
  }

  private Scenario scenario() throws Exception {
    Path root = Files.createDirectory(temp.resolve("install"));
    Path old = Files.createDirectories(root.resolve("releases/old"));
    UpgradeFiles.write(old.resolve("release.properties"), properties());
    Files.createSymbolicLink(root.resolve("current"), old);
    Path state = Files.createDirectory(temp.resolve("state"));
    String id = UUID.randomUUID().toString();
    Path incoming = Files.createDirectories(state.resolve("incoming").resolve(id));
    Files.copy(archive(), incoming.resolve("package.zip"));
    UpgradeFiles.write(incoming.resolve("options.properties"), new UpgradeOptions(false, 600, 90, "").properties());
    return new Scenario(root, state, id);
  }

  private static java.util.Properties properties() {
    java.util.Properties metadata = new java.util.Properties();
    metadata.setProperty("version", "3.3");
    metadata.setProperty("build", "1");
    return metadata;
  }

  private Path archive() throws Exception {
    java.util.Map<String, String> files = new java.util.LinkedHashMap<>();
    files.put("api/bin/blog-api", "#!/bin/sh\n");
    files.put("api/lib/app.jar", "jar");
    files.put("console/index.html", "console");
    files.put("desktop/index.html", "desktop");
    StringBuilder metadata = new StringBuilder("format=1\nproduct=today-blog\nversion=3.4\nbuild=2\nminimumJava=17\n");
    for (var file : files.entrySet()) {
      metadata.append("sha256.").append(file.getKey()).append('=')
              .append(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                      .digest(file.getValue().getBytes(StandardCharsets.UTF_8))))
              .append('\n');
    }
    files.put("META-INF/upgrade.properties", metadata.toString());
    Path archive = Files.createTempFile(temp, "package-", ".zip");
    try (var output = new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))) {
      for (var file : files.entrySet()) {
        output.putNextEntry(new java.util.zip.ZipEntry("today-blog/" + file.getKey()));
        output.write(file.getValue().getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
      }
    }
    return archive;
  }

  private record Scenario(Path root, Path state, String id) {
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
