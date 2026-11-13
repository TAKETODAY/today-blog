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

package cn.taketoday.blog.backup;

import infra.lang.Enumerable;

/**
 * 备份状态，类型名保留旧协议，枚举名即数据库存储值。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
public enum AppSnapshotStatus implements Enumerable<String> {

  /** 创建中 */
  CREATING,

  /** 可用 */
  AVAILABLE,

  /** 失败 */
  FAILED,

  /** 删除中 */
  DELETING,

  /** 已删除 */
  DELETED

}
