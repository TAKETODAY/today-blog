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

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.blog.updater.RuntimeLayout;
import cn.taketoday.blog.updater.UpgradeFiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 API 直接启动、停止及过期 PID 的处理。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
@EnabledOnOs(OS.LINUX)
class ProcessServiceControlTests {

  @TempDir
  Path root;

  @TempDir
  Path dataRoot;

  private final UpgradeFiles files = new UpgradeFiles();

  @Test
  void startsTheCurrentReleaseAndReplacesItsProcess() throws Exception {
    Path release = Files.createDirectories(root.resolve("releases/first"));
    Path bin = Files.createDirectories(release.resolve("api/bin"));
    Path launcher = Files.writeString(bin.resolve("blog-api"), """
            #!/bin/sh
            printf '%s\\n' "$BLOG_RELEASE_PATH" "$BLOG_DATA_ROOT" "$PWD"
            exec /bin/sleep 60
            """);
    assertThat(launcher.toFile().setExecutable(true)).isTrue();
    Files.createSymbolicLink(root.resolve("current"), release);
    Path pidFile = root.resolve("run/blog-api.pid");
    ProcessServiceControl service = service();
    service.start();
    try {
      Path log = dataRoot.resolve("logs/blog-api.log");
      String expected = String.join("\n", release.toString(), dataRoot.toString(), dataRoot.toString()) + "\n";
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (!Files.readString(log).contains(expected) && System.nanoTime() < deadline) {
        Thread.sleep(20);
      }
      assertThat(log).content().isEqualTo(expected);
      assertThat(root.resolve("logs")).doesNotExist();
      long oldPid = Long.parseLong(files.read(pidFile).getProperty("pid"));
      assertThatThrownBy(service::start).hasMessageContaining("已有 API PID 文件");
      service.restart();
      long newPid = Long.parseLong(files.read(pidFile).getProperty("pid"));
      assertThat(newPid).isNotEqualTo(oldPid);
      assertThat(ProcessHandle.of(oldPid)).isEmpty();
    }
    finally {
      service.stop();
    }
    assertThat(pidFile).doesNotExist();
  }

  @Test
  void neverSignalsAReusedPid() throws Exception {
    Process other = new ProcessBuilder("/bin/sleep", "60").start();
    Path pidFile = root.resolve("run/blog-api.pid");
    try {
      Properties stale = new Properties();
      stale.setProperty("pid", Long.toString(other.pid()));
      stale.setProperty("startedAt", Instant.EPOCH.toString());
      files.write(pidFile, stale);

      service().stop();

      assertThat(other.isAlive()).isTrue();
      assertThat(pidFile).doesNotExist();
    }
    finally {
      other.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void restartRequiresTheCurrentBuildInTheHealthResponse() throws Exception {
    Path release = Files.createDirectories(root.resolve("releases/first"));
    Path executable = Files.createDirectories(release.resolve("api/bin")).resolve("blog-api");
    Files.writeString(executable, "#!/bin/sh\nexec /bin/sleep 60\n");
    assertThat(executable.toFile().setExecutable(true)).isTrue();
    Files.writeString(release.resolve("release.properties"), "build=42\n");
    Files.createSymbolicLink(root.resolve("current"), release);

    var body = new AtomicReference<>("UP:42");
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/internal/health", exchange -> {
      byte[] bytes = body.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, bytes.length);
      try (var output = exchange.getResponseBody()) {
        output.write(bytes);
      }
    });
    server.start();
    var url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/internal/health");
    var service = new ProcessServiceControl(root, new RuntimeLayout(dataRoot), url, List.of(), files);
    try {
      service.restartAndWait(5);
      body.set("UP:41");
      assertThatThrownBy(() -> service.restartAndWait(1)).hasMessageContaining("预期构建号: 42");
    }
    finally {
      try {
        service.stop();
      }
      finally {
        server.stop(0);
      }
    }
  }

  private ProcessServiceControl service() {
    return new ProcessServiceControl(root, new RuntimeLayout(dataRoot),
            URI.create("http://127.0.0.1:8080/api/internal/health"), List.of(), files);
  }

}
