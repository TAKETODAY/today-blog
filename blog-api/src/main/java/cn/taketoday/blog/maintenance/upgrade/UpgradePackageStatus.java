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

package cn.taketoday.blog.maintenance.upgrade;

import org.jspecify.annotations.Nullable;

import infra.lang.Enumerable;

/**
 * 升级包状态。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public enum UpgradePackageStatus implements Enumerable<String> {

  /** 已上传，可用于升级。 */
  READY,

  /** 已被升级任务使用。 */
  CONSUMED;

  public static @Nullable UpgradePackageStatus of(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    for (UpgradePackageStatus status : values()) {
      if (status.name().equals(value)) {
        return status;
      }
    }
    return null;
  }

}
