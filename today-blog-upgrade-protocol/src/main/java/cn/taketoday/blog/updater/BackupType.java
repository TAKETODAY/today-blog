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

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 备份范围的共享协议，默认不包含 OSS。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/25
 */
public enum BackupType {
  DATABASE, ATTACHMENTS, WEBSITE, CONFIGURATION, RELEASE, OSS;

  public static Set<BackupType> recommended() {
    return EnumSet.of(DATABASE, ATTACHMENTS, WEBSITE, CONFIGURATION, RELEASE);
  }

  public static Set<BackupType> parse(String value) {
    if ("all".equals(value)) {
      return EnumSet.allOf(BackupType.class);
    }
    var types = EnumSet.noneOf(BackupType.class);
    for (String name : value.split(",", -1)) {
      if (!types.add(valueOf(name))) {
        throw new IllegalArgumentException("重复的备份类型");
      }
    }
    return types;
  }

  public static String encode(Set<BackupType> types) {
    if (types.isEmpty()) {
      throw new IllegalArgumentException("至少选择一种备份类型");
    }
    return types.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
  }
}
