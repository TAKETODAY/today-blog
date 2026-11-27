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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.blog.updater.UpgradeFiles;

/**
 * 升级器数据库连接配置。默认复用备份凭据文件中的 {@code [client]} 配置。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public record UpgradeDatabase(String url, String user, String password) {

  public static final Path DEFAULT_CONFIG = Path.of(
          System.getenv().getOrDefault("BLOG_UPGRADE_CONFIG", "/etc/today-blog/upgrade.properties"));

  static UpgradeDatabase read(Path configPath, UpgradeFiles files) throws IOException {
    files.verifyTrusted(configPath, false);
    Properties config = files.read(configPath);
    String database = config.getProperty("database");
    if (database == null || !database.matches("[A-Za-z0-9_]+")) {
      throw new IOException("升级器数据库名称无效");
    }
    String configuredUrl = config.getProperty("jdbcUrl");
    String user = config.getProperty("jdbcUser");
    String password = config.getProperty("jdbcPassword");
    String passwordFile = config.getProperty("jdbcPasswordFile");
    if (passwordFile != null) {
      Path secret = Path.of(passwordFile);
      files.verifyTrusted(secret, true);
      password = Files.readString(secret, StandardCharsets.UTF_8).trim();
    }
    if (configuredUrl != null) {
      if (user == null || password == null) {
        throw new IOException("升级器数据库连接配置不完整");
      }
      return new UpgradeDatabase(configuredUrl, user, password);
    }

    Path defaultsFile = Path.of(config.getProperty("defaultsFile", "/etc/today-blog/backup.cnf"));
    files.verifyTrusted(defaultsFile, true);
    Map<String, String> client = readClient(defaultsFile);
    String host = client.getOrDefault("host", "127.0.0.1");
    String port = client.getOrDefault("port", "3306");
    user = client.get("user");
    password = client.get("password");
    if (user == null || password == null) {
      throw new IOException("升级器数据库凭据不完整");
    }
    String url = "jdbc:mysql://" + host + ":" + port + "/" + database
            + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    return new UpgradeDatabase(url, user, password);
  }

  static Map<String, String> readClient(Path file) throws IOException {
    Map<String, String> values = new HashMap<>();
    boolean client = false;
    for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
        continue;
      }
      if (line.startsWith("[")) {
        client = line.equalsIgnoreCase("[client]");
        continue;
      }
      int index = line.indexOf('=');
      if (client && index > 0) {
        values.put(line.substring(0, index).strip(), line.substring(index + 1).strip());
      }
    }
    return values;
  }

}
