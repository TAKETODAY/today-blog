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

import java.util.Properties;

/**
 * API 和升级器共同验证的参数，不包含路径、命令或凭据。
 */
public record UpgradeOptions(boolean backupEnabled, int backupTimeoutSeconds, int healthTimeoutSeconds, String note) {

  public UpgradeOptions {
    if (backupTimeoutSeconds < 30 || backupTimeoutSeconds > 3600) {
      throw new IllegalArgumentException("备份超时必须在 30 至 3600 秒之间");
    }
    if (healthTimeoutSeconds < 10 || healthTimeoutSeconds > 600) {
      throw new IllegalArgumentException("健康检查超时必须在 10 至 600 秒之间");
    }
    if (note == null || note.length() > 500) {
      throw new IllegalArgumentException("升级备注不能超过 500 字");
    }
  }

  public static UpgradeOptions defaults() {
    return new UpgradeOptions(true, 600, 90, "");
  }

  public static UpgradeOptions from(Properties properties) {
    for (Object key : properties.keySet()) {
      if (!java.util.Set.of("backupEnabled", "backupTimeoutSeconds", "healthTimeoutSeconds", "note").contains(key)) {
        throw new IllegalArgumentException("包含不支持的升级参数");
      }
    }
    String enabled = properties.getProperty("backupEnabled", "true");
    if (!enabled.equals("true") && !enabled.equals("false")) {
      throw new IllegalArgumentException("数据库备份开关无效");
    }
    return new UpgradeOptions(Boolean.parseBoolean(enabled),
            Integer.parseInt(properties.getProperty("backupTimeoutSeconds", "600")),
            Integer.parseInt(properties.getProperty("healthTimeoutSeconds", "90")), properties.getProperty("note", ""));
  }

  public Properties properties() {
    Properties result = new Properties();
    result.setProperty("backupEnabled", Boolean.toString(backupEnabled));
    result.setProperty("backupTimeoutSeconds", Integer.toString(backupTimeoutSeconds));
    result.setProperty("healthTimeoutSeconds", Integer.toString(healthTimeoutSeconds));
    result.setProperty("note", note);
    return result;
  }
}
