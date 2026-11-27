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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

/**
 * 运行数据的保留策略和空间告警阈值。配置文件由 root 管理，默认位于
 * {@code /etc/today-blog/data.properties}。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/22
 */
public record DataPolicy(int backupsKeep, int packagesKeep, int logsDays,
        int warningUsedPercent, long warningFreeBytes) {

  public static final Path DEFAULT_CONFIG = Path.of("/etc/today-blog/data.properties");

  public static final String BACKUPS_KEEP = "backups.keep";
  public static final String PACKAGES_KEEP = "packages.keep";
  public static final String LOGS_DAYS = "logs.days";
  public static final String WARNING_USED_PERCENT = "warning.usedPercent";
  public static final String WARNING_FREE_BYTES = "warning.freeBytes";

  public DataPolicy {
    if (backupsKeep < 0 || backupsKeep > 1000) {
      throw new IllegalArgumentException("备份保留数量必须在 0 到 1000 之间");
    }
    if (packagesKeep < 0 || packagesKeep > 1000) {
      throw new IllegalArgumentException("升级包保留数量必须在 0 到 1000 之间");
    }
    if (logsDays < 0 || logsDays > 3650) {
      throw new IllegalArgumentException("日志保留天数必须在 0 到 3650 之间");
    }
    if (warningUsedPercent < 1 || warningUsedPercent > 100) {
      throw new IllegalArgumentException("空间告警百分比必须在 1 到 100 之间");
    }
    if (warningFreeBytes < 0) {
      throw new IllegalArgumentException("空间告警可用字节数不能为负数");
    }
  }

  public static DataPolicy defaults() {
    return new DataPolicy(5, 3, 14, 85, 1024L * 1024 * 1024);
  }

  public static DataPolicy load(Path config) throws IOException {
    if (!Files.isRegularFile(config)) {
      return defaults();
    }
    Properties values = new Properties();
    try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
      values.load(reader);
    }
    return parse(values);
  }

  public static DataPolicy parse(Properties values) {
    DataPolicy defaults = defaults();
    return new DataPolicy(
            integer(values, BACKUPS_KEEP, defaults.backupsKeep),
            integer(values, PACKAGES_KEEP, defaults.packagesKeep),
            integer(values, LOGS_DAYS, defaults.logsDays),
            integer(values, WARNING_USED_PERCENT, defaults.warningUsedPercent),
            longValue(values, WARNING_FREE_BYTES, defaults.warningFreeBytes));
  }

  private static int integer(Properties values, String key, int defaultValue) {
    String value = values.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    }
    catch (NumberFormatException invalid) {
      throw new IllegalArgumentException("配置项 " + key + " 必须是整数");
    }
  }

  private static long longValue(Properties values, String key, long defaultValue) {
    String value = values.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    }
    catch (NumberFormatException invalid) {
      throw new IllegalArgumentException("配置项 " + key + " 必须是整数");
    }
  }

  public DataPolicy withBackupsKeep(int value) {
    return new DataPolicy(value, packagesKeep, logsDays, warningUsedPercent, warningFreeBytes);
  }

  public DataPolicy withPackagesKeep(int value) {
    return new DataPolicy(backupsKeep, value, logsDays, warningUsedPercent, warningFreeBytes);
  }

  public DataPolicy withLogsDays(int value) {
    return new DataPolicy(backupsKeep, packagesKeep, value, warningUsedPercent, warningFreeBytes);
  }

  public DataPolicy withWarningUsedPercent(int value) {
    return new DataPolicy(backupsKeep, packagesKeep, logsDays, value, warningFreeBytes);
  }

  public DataPolicy withWarningFreeBytes(long value) {
    return new DataPolicy(backupsKeep, packagesKeep, logsDays, warningUsedPercent, value);
  }

  public Properties toProperties() {
    Properties values = new Properties();
    values.setProperty(BACKUPS_KEEP, Integer.toString(backupsKeep));
    values.setProperty(PACKAGES_KEEP, Integer.toString(packagesKeep));
    values.setProperty(LOGS_DAYS, Integer.toString(logsDays));
    values.setProperty(WARNING_USED_PERCENT, Integer.toString(warningUsedPercent));
    values.setProperty(WARNING_FREE_BYTES, Long.toString(warningFreeBytes));
    return values;
  }

  public void store(Path config) throws IOException {
    Path parent = config.getParent();
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, ".data-policy-", ".tmp");
    try {
      try (OutputStream output = Files.newOutputStream(temporary)) {
        toProperties().store(output, "today-blog data policy");
      }
      try {
        Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-r--r--"));
      }
      catch (UnsupportedOperationException ignored) {
        // 非 POSIX 文件系统沿用默认权限。
      }
      Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    finally {
      Files.deleteIfExists(temporary);
    }
  }

}
