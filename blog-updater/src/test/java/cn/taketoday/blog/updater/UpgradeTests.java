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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeTests {

  @TempDir
  Path temp;

  KeyPair keys;

  Path publicKey;

  @BeforeEach
  void keyPair() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    keys = generator.generateKeyPair();
    publicKey = temp.resolve("public.pem");
    Files.writeString(publicKey, "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n");
  }

  @Test
  void acceptsCompleteSignedPackageAndExtracts() throws Exception {
    Path archive = archive(null, false, false);
    var manifest = UpgradePackage.verify(archive, publicKey);
    Path destination = Files.createDirectory(temp.resolve("extracted"));
    UpgradePackage.extract(archive, destination, manifest);
    assertThat(manifest.version()).isEqualTo("3.4");
    assertThat(Files.readString(destination.resolve("console/index.html"))).isEqualTo("console");
  }

  @Test
  void rejectsInvalidSignature() throws Exception {
    assertThatThrownBy(() -> UpgradePackage.verify(archive(null, true, false), publicKey)).hasMessageContaining("签名");
  }

  @Test
  void rejectsTamperedFile() throws Exception {
    assertThatThrownBy(() -> UpgradePackage.verify(archive(null, false, true), publicKey)).hasMessageContaining("摘要不匹配");
  }

  @Test
  void rejectsUnsignedFileAndTraversal() throws Exception {
    assertThatThrownBy(() -> UpgradePackage.verify(archive("console/extra.js", false, false), publicKey))
            .hasMessageContaining("未签名文件");
    assertThatThrownBy(() -> UpgradePackage.verify(archive("../outside", false, false), publicKey))
            .hasMessageContaining("非法路径");
  }

  @Test
  void installsAndVerifiesNewRelease() throws Exception {
    Scenario scenario = scenario();
    FakeService service = new FakeService(false, false);
    BlogUpdaterApplication.run(scenario.root, scenario.state, publicKey, scenario.id, service);
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("3.4-2");
    assertThat(result(scenario)).isEqualTo("SUCCESS");
    assertThat(service.restarts).isEqualTo(1);
    assertThat(UpgradeFiles.read(scenario.root.resolve("highest-build.properties")).getProperty("build")).isEqualTo("2");
  }

  @Test
  void failedHealthCheckRestoresOldRelease() throws Exception {
    Scenario scenario = scenario();
    FakeService service = new FakeService(true, false);
    BlogUpdaterApplication.run(scenario.root, scenario.state, publicKey, scenario.id, service);
    assertThat(scenario.root.resolve("current").toRealPath().getFileName().toString()).isEqualTo("old");
    assertThat(result(scenario)).isEqualTo("ROLLED_BACK");
    assertThat(service.restarts).isEqualTo(2);
  }

  @Test
  void failedRollbackIsReportedAsFailure() throws Exception {
    Scenario scenario = scenario();
    assertThatThrownBy(() -> BlogUpdaterApplication.run(scenario.root, scenario.state, publicKey, scenario.id,
            new FakeService(true, true))).hasMessageContaining("健康检查失败");
    assertThat(result(scenario)).isEqualTo("FAILED");
  }

  @Test
  void rejectsPreviouslySuccessfulBuildWithoutRestart() throws Exception {
    Scenario scenario = scenario();
    Properties highest = new Properties();
    highest.setProperty("build", "2");
    UpgradeFiles.write(scenario.root.resolve("highest-build.properties"), highest);
    FakeService service = new FakeService(false, false);
    assertThatThrownBy(() -> BlogUpdaterApplication.run(scenario.root, scenario.state, publicKey, scenario.id, service))
            .hasMessageContaining("降级");
    assertThat(service.restarts).isZero();
    assertThat(result(scenario)).isEqualTo("FAILED");
  }

  @Test
  void rejectsInvalidTaskId() {
    assertThatThrownBy(() -> UpgradeFiles.checkId("../../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
  }

  private Scenario scenario() throws Exception {
    Path root = Files.createDirectory(temp.resolve("install"));
    Path old = Files.createDirectories(root.resolve("releases/old"));
    Properties metadata = new Properties();
    metadata.setProperty("version", "3.3");
    metadata.setProperty("build", "1");
    UpgradeFiles.write(old.resolve("release.properties"), metadata);
    Files.createSymbolicLink(root.resolve("current"), old);
    Path state = Files.createDirectory(temp.resolve("state"));
    String id = UUID.randomUUID().toString();
    Path incoming = Files.createDirectories(state.resolve("incoming").resolve(id));
    Files.copy(archive(null, false, false), incoming.resolve("package.zip"));
    return new Scenario(root, state, id);
  }

  private String result(Scenario scenario) throws Exception {
    return UpgradeFiles.read(scenario.state.resolve("results").resolve(scenario.id + ".properties")).getProperty("status");
  }

  private record Scenario(Path root, Path state, String id) { }

  private static class FakeService implements BlogUpdaterApplication.ServiceControl {
    final boolean failNew;
    final boolean failOld;
    int restarts;

    FakeService(boolean failNew, boolean failOld) { this.failNew = failNew; this.failOld = failOld; }

    @Override
    public void restart() { restarts++; }

    @Override
    public boolean healthy(long build) { return build == 2 ? !failNew : !failOld; }
  }

  private Path archive(String extra, boolean badSignature, boolean corruptFile) throws Exception {
    Map<String, String> files = new LinkedHashMap<>();
    files.put("api/bin/blog-api", "#!/bin/sh\n");
    files.put("api/lib/app.jar", "jar");
    files.put("console/index.html", "console");
    files.put("desktop/index.html", "desktop");
    StringBuilder metadata = new StringBuilder("format=1\nproduct=today-blog\nversion=3.4\nbuild=2\nminimumJava=17\nrollbackSafe=true\n");
    for (var file : files.entrySet()) {
      metadata.append("sha256.").append(file.getKey()).append('=')
              .append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getValue().getBytes(StandardCharsets.UTF_8))))
              .append('\n');
    }
    Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(keys.getPrivate());
    signer.update(metadata.toString().getBytes(StandardCharsets.UTF_8));
    byte[] signature = signer.sign();
    if (badSignature)
      signature[0] ^= 1;
    files.put("META-INF/upgrade.properties", metadata.toString());
    files.put("META-INF/upgrade.sig", Base64.getEncoder().encodeToString(signature));
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
}
