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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.updater.UpgradeFiles;

/**
 * 使用 systemd 管理固定的 API unit，不使用直接进程模式的 PID 文件。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
final class SystemdServiceControl extends ApiServiceControl {

  private static final String UNIT = "today-blog.service";

  private ProcessHandle started;

  SystemdServiceControl(Path root, URI healthUrl, UpgradeFiles files) {
    super(root, healthUrl, files);
  }

  @Override
  void start() throws Exception {
    systemctl("start", null);
  }

  @Override
  void stop() throws Exception {
    started = null;
    systemctl("stop", null);
  }

  @Override
  public void restart() throws Exception {
    started = null;
    // 仅清除失败单元的限流：处于 inactive 的单元可能已经被 systemd 卸载。
    if ("failed".equals(systemctl("show", "ActiveState"))) {
      systemctl("reset-failed", null);
    }
    systemctl("restart", null);
    long pid = Long.parseLong(systemctl("show", "MainPID"));
    if (pid > 1) {
      started = ProcessHandle.of(pid).orElse(null);
    }
  }

  @Override
  boolean isStartedProcessAlive() {
    return started != null && started.isAlive();
  }

  private String systemctl(String action, String property) throws IOException, InterruptedException {
    ProcessBuilder command = new ProcessBuilder("/usr/bin/systemctl", "--system", "--no-pager", action, UNIT);
    if (property != null) {
      command.command().add("--property=" + property);
      command.command().add("--value");
    }
    Process process = command.redirectInput(Path.of("/dev/null").toFile())
            .redirectOutput(property != null ? ProcessBuilder.Redirect.PIPE : ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    try (var output = process.getInputStream()) {
      if (!process.waitFor(75, TimeUnit.SECONDS)) {
        throw new IOException("systemctl " + action + " " + UNIT + " 超时");
      }
      if (process.exitValue() != 0) {
        throw new IOException("systemctl " + action + " " + UNIT + " 失败，退出码: " + process.exitValue());
      }
      return property != null ? new String(output.readAllBytes(), StandardCharsets.UTF_8).trim() : "";
    }
    finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

}
