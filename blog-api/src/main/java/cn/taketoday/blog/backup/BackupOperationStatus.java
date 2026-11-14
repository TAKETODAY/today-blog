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

import org.jspecify.annotations.Nullable;

import infra.lang.Enumerable;

/**
 * 备份及关联升级的操作状态，枚举名即数据库存储值。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
public enum BackupOperationStatus implements Enumerable<String> {

  /** 已提交 */
  STARTING,

  /** 校验升级包 */
  VALIDATING,

  /** 准备版本 */
  INSTALLING,

  /** 数据库备份 */
  BACKING_UP,

  /** 切换并重启 */
  RESTARTING,

  /** 健康检查 */
  HEALTH_CHECKING,

  /** 执行成功 */
  SUCCESS,

  /** 执行失败 */
  FAILED,

  /** 正在恢复旧版本 */
  ROLLING_BACK,

  /** 已恢复旧版本 */
  ROLLED_BACK,

  /** 待确认 */
  READY,

  /** 正在恢复备份 */
  RESTORING,

  /** 恢复失败，保持维护状态等待人工处理 */
  MAINTENANCE_REQUIRED;

  /**
   * 按数据库或元数据中的状态名解析，未知值返回 {@code null}。
   */
  public static @Nullable BackupOperationStatus of(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    for (BackupOperationStatus status : values()) {
      if (status.name().equals(value)) {
        return status;
      }
    }
    return null;
  }

}
