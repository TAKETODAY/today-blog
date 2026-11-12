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
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * 设置与任务独立于业务数据库存储，程序回滚不改变其格式。
 */
public final class UpgradeStore {

  private final Path state;

  public UpgradeStore(Path state) {
    this.state = state;
  }

  public UpgradeOptions settings() throws IOException {
    Path path = state.resolve("incoming/settings.properties");
    return Files.exists(path) ? UpgradeOptions.from(UpgradeFiles.read(path)) : UpgradeOptions.defaults();
  }

  public void saveSettings(UpgradeOptions options) throws IOException {
    UpgradeFiles.write(state.resolve("incoming/settings.properties"), options.properties());
  }

  public void snapshot(String id, UpgradeOptions options, long operator, Properties current) throws IOException {
    UpgradeFiles.checkId(id);
    Path task = state.resolve("incoming").resolve(id);
    Properties metadata = UpgradeFiles.read(task.resolve("metadata.properties"));
    if (!"READY".equals(metadata.getProperty("status"))) {
      throw new IllegalStateException("该任务已提交，不可重复启动");
    }
    UpgradeFiles.write(task.resolve("options.properties"), options.properties());
    metadata.putAll(options.properties());
    metadata.setProperty("operatorId", Long.toString(operator));
    metadata.setProperty("oldVersion", current.getProperty("version", ""));
    metadata.setProperty("oldBuild", current.getProperty("build", ""));
    metadata.setProperty("startedAt", Instant.now().toString());
    metadata.setProperty("status", "STARTING");
    metadata.setProperty("message", "任务已提交，请等待升级器启动");
    UpgradeFiles.write(task.resolve("metadata.properties"), metadata);
  }

  public Properties task(String id) throws IOException {
    UpgradeFiles.checkId(id);
    Properties result = UpgradeFiles.read(state.resolve("incoming").resolve(id).resolve("metadata.properties"));
    Path status = state.resolve("results").resolve(id + ".properties");
    if (Files.exists(status)) {
      result.putAll(UpgradeFiles.read(status));
    }
    return result;
  }

  public List<Properties> history() throws IOException {
    Path incoming = state.resolve("incoming");
    if (!Files.isDirectory(incoming)) {
      return List.of();
    }
    try (var paths = Files.list(incoming)) {
      List<Path> tasks = paths.filter(path -> path.getFileName().toString().matches(
              "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
              .filter(path -> Files.isRegularFile(path.resolve("metadata.properties"))).toList();
      java.util.ArrayList<Properties> result = new java.util.ArrayList<>();
      for (Path path : tasks) {
        result.add(task(path.getFileName().toString()));
      }
      result.sort(Comparator.comparing((Properties item) -> item.getProperty("createdAt", "")).reversed());
      return result;
    }
  }
}
