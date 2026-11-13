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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/**
 * API 与升级器之间的维护状态协议。
 * <p>
 * 文件存在表示维护中，API 会拒绝新的业务请求但保持进程运行。管理端开始升级的
 * HTTP 请求会立即写入该文件，升级器在版本切换完成并通过健康检查后删除；
 * 手动开启的维护状态会保留到管理员主动关闭。
 */
public final class MaintenanceState {

  public static final String FILE_NAME = "maintenance.properties";

  public static final String DEFAULT_RUNTIME_PATH = "/var/lib/today-blog";

  // 系统正在升级，请稍后重试
  public static final String DEFAULT_MESSAGE = "系统升级中，请稍后访问";

  public static final int DEFAULT_RETRY_AFTER_SECONDS = 120;

  private MaintenanceState() {
  }

  /**
   * 状态文件路径，可由 {@code BLOG_MAINTENANCE_FILE} 或 {@code BLOG_RUNTIME_PATH} 覆盖。
   */
  public static Path defaultFile() {
    String configured = System.getenv("BLOG_MAINTENANCE_FILE");
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured);
    }
    String runtime = System.getenv().getOrDefault("BLOG_RUNTIME_PATH", DEFAULT_RUNTIME_PATH);
    return Path.of(runtime).resolve(FILE_NAME);
  }

  public static boolean enabled(Path file) {
    return Files.exists(file);
  }

  public static Source source(Path file) {
    if (!Files.exists(file)) {
      return null;
    }
    try {
      return Source.of(UpgradeFiles.read(file).getProperty("source"));
    }
    catch (Exception unreadable) {
      // 无法判断来源时按升级流程管理，避免升级失败后长期停留在维护状态。
      return Source.UPGRADE;
    }
  }

  public static void enable(Path file, String message, Source source) throws IOException {
    enable(file, message, DEFAULT_RETRY_AFTER_SECONDS, source);
  }

  public static void enable(Path file, String message, int retryAfterSeconds, Source source) throws IOException {
    Properties properties = new Properties();
    properties.setProperty("enabled", "true");
    properties.setProperty("message", message);
    properties.setProperty("retryAfter", Integer.toString(retryAfterSeconds));
    properties.setProperty("source", source.value);
    properties.setProperty("startedAt", Instant.now().toString());
    UpgradeFiles.write(file, properties);
  }

  public static void disable(Path file) throws IOException {
    Files.deleteIfExists(file);
  }

  /**
   * 维护状态来源。
   */
  public enum Source {

    /** 管理端开始升级时写入，升级结束后由升级器删除。 */
    UPGRADE("upgrade"),

    /** 管理员手动开启，升级流程不会自动删除。 */
    MANUAL("manual");

    private final String value;

    Source(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }

    public static Source of(String value) {
      for (Source source : values()) {
        if (source.value.equals(value)) {
          return source;
        }
      }
      return null;
    }
  }
}
