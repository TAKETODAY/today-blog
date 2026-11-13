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

package cn.taketoday.blog.web.maintenance;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import cn.taketoday.blog.updater.MaintenanceState;
import cn.taketoday.blog.updater.UpgradeFiles;
import infra.beans.factory.annotation.Autowired;
import infra.core.env.Environment;
import infra.stereotype.Component;

/**
 * 读取并修改升级器共享的维护状态文件。
 * <p>
 * 文件不存在表示正常服务；文件存在表示维护中。状态文件每秒重新读取一次，
 * 因此 HTTP 请求切换维护状态后无需重启 API 即可生效。
 */
@Component
public final class MaintenanceMode {

  static final String FILE_PROPERTY = "blog.maintenance.file";

  private static final long REFRESH_NANOS = TimeUnit.SECONDS.toNanos(1);

  private static final Status NORMAL = new Status(false, null, 0, null);

  private final Path marker;

  private volatile @Nullable Status cached;

  private volatile long refreshedAt;

  @Autowired
  MaintenanceMode(Environment environment) {
    this(resolveMarker(environment));
  }

  MaintenanceMode(Path marker) {
    this.marker = marker;
  }

  /**
   * 当前维护状态。
   */
  public Status status() {
    long now = System.nanoTime();
    Status current = cached;
    if (current == null || now - refreshedAt >= REFRESH_NANOS) {
      current = load();
      this.cached = current;
      this.refreshedAt = now;
    }
    return current;
  }

  public boolean enabled() {
    return status().enabled();
  }

  /**
   * 直接检查状态文件是否存在，跳过请求路径使用的短缓存。
   */
  public boolean exists() {
    return MaintenanceState.enabled(marker);
  }

  /**
   * 开启维护状态。{@link MaintenanceState.Source#UPGRADE} 会在升级结束后被自动清除，
   * {@link MaintenanceState.Source#MANUAL} 需要管理员主动关闭。
   */
  public synchronized void enable(String message, int retryAfterSeconds, MaintenanceState.Source source) throws IOException {
    MaintenanceState.enable(marker, message, retryAfterSeconds, source);
    invalidate();
  }

  public synchronized void disable() throws IOException {
    MaintenanceState.disable(marker);
    invalidate();
  }

  private void invalidate() {
    this.cached = null;
    this.refreshedAt = 0;
  }

  private Status load() {
    if (!Files.exists(marker)) {
      return NORMAL;
    }
    try {
      Properties properties = UpgradeFiles.read(marker);
      String message = properties.getProperty("message", MaintenanceState.DEFAULT_MESSAGE);
      int retryAfter = parseRetryAfter(properties.getProperty("retryAfter"));
      if (message.isBlank()) {
        message = MaintenanceState.DEFAULT_MESSAGE;
      }
      MaintenanceState.Source source = MaintenanceState.Source.of(properties.getProperty("source"));
      return new Status(true, message, retryAfter, source);
    }
    catch (Exception unreadable) {
      // 状态文件存在但暂时无法读取时按维护处理，避免升级期间继续接受请求。
      return new Status(true, MaintenanceState.DEFAULT_MESSAGE,
              MaintenanceState.DEFAULT_RETRY_AFTER_SECONDS, MaintenanceState.Source.UPGRADE);
    }
  }

  private static int parseRetryAfter(@Nullable String value) {
    if (value == null) {
      return MaintenanceState.DEFAULT_RETRY_AFTER_SECONDS;
    }
    try {
      int seconds = Integer.parseInt(value);
      return seconds > 0 ? seconds : MaintenanceState.DEFAULT_RETRY_AFTER_SECONDS;
    }
    catch (NumberFormatException ignored) {
      return MaintenanceState.DEFAULT_RETRY_AFTER_SECONDS;
    }
  }

  private static Path resolveMarker(Environment environment) {
    String file = environment.getProperty(FILE_PROPERTY);
    if (file != null && !file.isBlank()) {
      return Path.of(file);
    }
    return MaintenanceState.defaultFile();
  }

  /**
   * 维护状态快照。
   *
   * @param enabled 是否处于维护中
   * @param message 返回给客户端的提示，正常服务时为 {@code null}
   * @param retryAfterSeconds 建议客户端重试等待的秒数
   * @param source 维护状态来源，正常服务时为 {@code null}
   */
  public record Status(boolean enabled, @Nullable String message, int retryAfterSeconds,
          MaintenanceState.@Nullable Source source) {

  }
}
