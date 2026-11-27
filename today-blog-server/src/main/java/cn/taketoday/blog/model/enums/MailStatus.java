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

package cn.taketoday.blog.model.enums;

import infra.lang.Enumerable;

/**
 * 邮件发送状态，枚举名即数据库存储值。
 *
 * <p>与 {@link AttachmentType} 一样只实现 {@link Enumerable}，不声明取值字段，
 * {@code getValue()} 默认返回枚举名，因此无需 {@code @EnumerationValue}。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/12
 */
public enum MailStatus implements Enumerable<String> {

  /** 待发送 */
  PENDING,

  /** 发送中 */
  SENDING,

  /** 已发送 */
  SENT,

  /** 发送失败 */
  FAILED

}
