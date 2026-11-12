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

package cn.taketoday.blog.snapshot;

import infra.lang.Enumerable;

/**
 * 应用操作类型，枚举名即数据库存储值。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
public enum AppOperationType implements Enumerable<String> {

  /** 系统更新 */
  UPGRADE,

  /** 创建快照 */
  SNAPSHOT_CREATE,

  /** 恢复快照 */
  SNAPSHOT_RESTORE,

  /** 删除快照 */
  SNAPSHOT_DELETE,

  /** 修改快照保护状态 */
  SNAPSHOT_PROTECT

}
