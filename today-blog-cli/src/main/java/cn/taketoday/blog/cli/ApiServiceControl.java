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
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.updater.UpgradeFiles;

/**
 * API 启停方式共用的版本定位和就绪检查。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
abstract class ApiServiceControl implements UpgradeExecutor.ServiceControl {

  private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");

  protected final Path root;

  private final URI healthUrl;

  private final UpgradeFiles files;

  ApiServiceControl(Path root, URI healthUrl, UpgradeFiles files) {
    this.root = root.toAbsolutePath().normalize();
    this.healthUrl = healthUrl;
    this.files = files;
    if (!"http".equals(healthUrl.getScheme()) || healthUrl.getHost() == null
            || !LOOPBACK_HOSTS.contains(healthUrl.getHost())) {
      throw new IllegalArgumentException("健康检查必须使用本机 HTTP 地址");
    }
  }

  abstract void start() throws Exception;

  abstract void stop() throws Exception;

  abstract boolean isStartedProcessAlive();

  public final void restartAndWait(int timeoutSeconds) throws Exception {
    long build = Long.parseLong(files.read(currentRelease().resolve("release.properties")).getProperty("build"));
    restart();
    if (!healthy(build, timeoutSeconds)) {
      throw new IOException("API 重启后健康检查失败，预期构建号: " + build);
    }
  }

  @Override
  public final boolean healthy(long build, int timeoutSeconds) throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    long deadline = System.nanoTime() + Duration.ofSeconds(timeoutSeconds).toNanos();
    long remaining;
    while (isStartedProcessAlive() && (remaining = deadline - System.nanoTime()) > 0) {
      try {
        var response = client.send(HttpRequest.newBuilder(healthUrl)
                        .timeout(Duration.ofNanos(Math.min(remaining, TimeUnit.SECONDS.toNanos(3))))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 && response.body().trim().equals("UP:" + build) && isStartedProcessAlive()) {
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

  final Path currentRelease() throws IOException {
    Path release = root.resolve("current").toRealPath();
    if (!root.resolve("releases").toRealPath().equals(release.getParent())) {
      throw new IOException("current 未指向受管理的发行目录");
    }
    return release;
  }

}
