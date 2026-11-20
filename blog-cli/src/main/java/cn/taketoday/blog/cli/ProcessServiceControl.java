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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.taketoday.blog.updater.UpgradeFiles;

/**
 * 直接管理 blog-api 子进程，不依赖 systemd。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
public final class ProcessServiceControl extends ApiServiceControl {

  private final Path state;

  private final Path workDirectory;

  private final List<String> launcherPrefix;

  private final UpgradeFiles files;

  private Process started;

  public ProcessServiceControl(Path root, Path state, Path workDirectory, URI healthUrl,
          List<String> launcherPrefix, UpgradeFiles files) {
    super(root, healthUrl, files);
    this.state = state.toAbsolutePath().normalize();
    this.workDirectory = workDirectory.toAbsolutePath().normalize();
    this.launcherPrefix = List.copyOf(launcherPrefix);
    this.files = files;
  }

  @Override
  public void restart() throws Exception {
    stop();
    start();
  }

  @Override
  boolean isStartedProcessAlive() {
    return started != null && started.isAlive();
  }

  @Override
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

  @Override
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

}
