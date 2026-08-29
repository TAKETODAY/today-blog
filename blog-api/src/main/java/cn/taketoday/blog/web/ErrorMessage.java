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

package cn.taketoday.blog.web;

import org.jspecify.annotations.Nullable;

import infra.core.style.ToStringBuilder;

/**
 * 错误消息封装类，用于表示操作失败时的错误信息。
 * <p>
 * 实现了 {@link HttpResult} 接口，通常作为 HTTP 响应体的一部分返回给客户端。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-04-16 14:23
 */
public final class ErrorMessage implements HttpResult {

  public static final ErrorMessage unauthorized = ErrorMessage.failed("登录超时");

  @Nullable
  private final String message;

  ErrorMessage(@Nullable String message) {
    this.message = message;
  }

  public static ErrorMessage failed(@Nullable String message) {
    return new ErrorMessage(message);
  }

  @Nullable
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("message", message)
            .toString();
  }

}
