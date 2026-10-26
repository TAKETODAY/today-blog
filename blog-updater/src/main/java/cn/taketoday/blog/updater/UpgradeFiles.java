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
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** API 与独立升级模块之间的磁盘协议。 */
public final class UpgradeFiles {

  private UpgradeFiles() { }

  public static void checkId(String id) {
    if (!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
      throw new IllegalArgumentException("升级任务 ID 无效");
    }
  }

  public static Properties read(Path path) throws IOException {
    Properties properties = new Properties();
    try (var input = Files.newInputStream(path)) {
      properties.load(input);
    }
    return properties;
  }

  public static void write(Path path, Properties properties) throws IOException {
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), ".status-", ".tmp");
    try {
      try (var output = Files.newOutputStream(temp)) { properties.store(output, null); }
      temp.toFile().setReadable(true, false);
      Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    finally { Files.deleteIfExists(temp); }
  }

  public static void status(Path results, String id, String state, String message) throws IOException {
    checkId(id);
    Properties properties = new Properties();
    properties.setProperty("id", id);
    properties.setProperty("status", state);
    properties.setProperty("message", message);
    properties.setProperty("updatedAt", java.time.Instant.now().toString());
    write(results.resolve(id + ".properties"), properties);
  }
}
