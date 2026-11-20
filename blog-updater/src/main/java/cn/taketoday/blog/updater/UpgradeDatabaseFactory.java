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

import infra.stereotype.Component;

/**
 * 创建升级器使用的数据库连接配置。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/20
 */
@Component
public final class UpgradeDatabaseFactory {

  private final UpgradeFiles files;

  public UpgradeDatabaseFactory(UpgradeFiles files) {
    this.files = files;
  }

  public UpgradeDatabase configured() throws IOException {
    return UpgradeDatabase.read(UpgradeDatabase.DEFAULT_CONFIG, files);
  }

}
