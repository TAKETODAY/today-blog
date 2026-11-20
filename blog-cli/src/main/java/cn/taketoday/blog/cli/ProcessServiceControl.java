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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.blog.updater.UpgradeFiles;

/**
 * 直接管理 blog-api 子进程，不依赖 systemd。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
public final class ProcessServiceControl implements UpgradeExecutor.ServiceControl {

  private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");

  private final Path root;

  private final URI healthUrl;

  private final Path state;

  private final Path workDirectory;

  private final List<String> launcherPrefix;

  private final UpgradeFiles files;

  private Process started;

  public ProcessServiceControl(Path root, Path state, Path workDirectory, URI healthUrl,
          List<String> launcherPrefix, UpgradeFiles files) {
    this.root = root.toAbsolutePath().normalize();
    this.state = state.toAbsolutePath().normalize();
    this.workDirectory = workDirectory.toAbsolutePath().normalize();
    this.healthUrl = healthUrl;
    this.launcherPrefix = List.copyOf(launcherPrefix);
    this.files = files;
    if (!"http".equals(healthUrl.getScheme()) || healthUrl.getHost() == null
            || !LOOPBACK_HOSTS.contains(healthUrl.getHost())) {
      throw new IllegalArgumentException("健康检查必须使用本机 HTTP 地址");
    }
  }

  @Override
  public void restart() throws Exception {
    stop();
    start();
  }

  void restartAndWait(int timeoutSeconds) throws Exception {
    long build = Long.parseLong(files.read(currentRelease().resolve("release.properties")).getProperty("build"));
    restart();
    if (!healthy(build, timeoutSeconds)) {
      throw new IOException("API 重启后健康检查失败，预期构建号: " + build);
    }
  }

  @Override
  public boolean healthy(long build, int timeoutSeconds) throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    long deadline = System.nanoTime() + Duration.ofSeconds(timeoutSeconds).toNanos();
    long remaining;
    while (started != null && started.isAlive() && (remaining = deadline - System.nanoTime()) > 0) {
      try {
        var response = client.send(HttpRequest.newBuilder(healthUrl)
                        .timeout(Duration.ofNanos(Math.min(remaining, TimeUnit.SECONDS.toNanos(3))))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && response.body().trim().equals("UP:" + build) && started.isAlive()) {
          return true;
        }
      }
      catch (IOException ignored) {
      }
      remaining = deadline - System.nanoTime();
      if (remaining > 0) {
        TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(500)));
      }
    }
    return false;
  }

  void stop() throws Exception {
    Path pidFile = root.resolve("run/blog-api.pid");
    if (Files.notExists(pidFile, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    if (!Files.isRegularFile(pidFile, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("API PID 文件不是普通文件");
    }
    Properties identity = files.read(pidFile);
    long pid = Long.parseLong(identity.getProperty("pid"));
    var startTime = java.time.Instant.parse(identity.getProperty("startedAt"));
    ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
    if (handle == null || !handle.isAlive()) {
      Files.deleteIfExists(pidFile);
      return;
    }
    var actualStart = handle.info().startInstant();
    if (actualStart.isPresent() && !actualStart.get().equals(startTime)) {
      // PID 已被复用，不向无关进程发送信号。
      Files.deleteIfExists(pidFile);
      return;
    }
    if (pid <= 1 || pid == ProcessHandle.current().pid() || actualStart.isEmpty()) {
      throw new IOException("无法确认 API 进程身份");
    }
    handle.destroy();
    try {
      handle.onExit().get(30, TimeUnit.SECONDS);
    }
    catch (TimeoutException gracefulTimeout) {
      handle.destroyForcibly();
      try {
        handle.onExit().get(10, TimeUnit.SECONDS);
      }
      catch (TimeoutException forceTimeout) {
        throw new IOException("API 进程无法停止", forceTimeout);
      }
    }
    Files.deleteIfExists(pidFile);
  }

  void start() throws IOException {
    if (!Files.notExists(root.resolve("run/blog-api.pid"), LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("已有 API PID 文件；如需重启，请执行 today-blog restart");
    }
    Path release = currentRelease();
    Path executable = release.resolve("api/bin/blog-api");
    if (!Files.isExecutable(executable)) {
      throw new IOException("当前发行版缺少可执行的 blog-api");
    }
    Path run = Files.createDirectories(root.resolve("run"));
    Path logs = Files.createDirectories(root.resolve("logs"));
    var command = new ArrayList<>(launcherPrefix);
    command.add(executable.toString());
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.environment().put("BLOG_RELEASE_PATH", release.toString());
    builder.environment().put("BLOG_INSTALL_PATH", root.toString());
    builder.environment().put("BLOG_UPGRADE_STATE", state.toString());
    Process process = builder
            .directory(workDirectory.toFile())
            .redirectInput(Path.of("/dev/null").toFile())
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logs.resolve("blog-api.log").toFile()))
            .redirectErrorStream(true)
            .start();
    try {
      var startedAt = process.info().startInstant().orElseThrow(() -> new IOException("无法获取 API 进程启动时间"));
      Properties identity = new Properties();
      identity.setProperty("pid", Long.toString(process.pid()));
      identity.setProperty("startedAt", startedAt.toString());
      files.write(run.resolve("blog-api.pid"), identity);
      started = process;
    }
    catch (Exception failure) {
      process.destroyForcibly();
      throw failure;
    }
  }

  private Path currentRelease() throws IOException {
    Path release = root.resolve("current").toRealPath();
    if (!root.resolve("releases").toRealPath().equals(release.getParent())) {
      throw new IOException("current 未指向受管理的发行目录");
    }
    return release;
  }

}
