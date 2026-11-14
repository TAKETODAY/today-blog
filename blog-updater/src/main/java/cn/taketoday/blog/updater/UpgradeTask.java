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

import java.util.List;

/**
 * 升级器从数据库读取的任务快照。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public record UpgradeTask(
        long id,
        long packageId,
        String packagePath,
        String version,
        long build,
        Long oldBuild,
        boolean backupEnabled,
        int backupTimeoutSeconds,
        int healthTimeoutSeconds,
        boolean deletePackageAfterUpgrade,
        List<ReleaseMapping> mappings) {

}
