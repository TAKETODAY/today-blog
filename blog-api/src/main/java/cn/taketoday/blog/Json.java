/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog;

import java.util.function.Function;

import cn.taketoday.core.style.ToStringBuilder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-17 17:46
 */
public final class Json implements Result {

  private Object data;
  private String message;
  private boolean success;

  @Override
  public Object getData() {
    return data;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public Json data(Object data) {
    this.data = data;
    return this;
  }

  public Json message(String message) {
    this.message = message;
    return this;
  }

  public Json success(boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Apply the common {@link Json} result
   *
   * @param func the {@link Function}
   * @param param parameter
   */
  public static <T> Json apply(Function<T, Boolean> func, T param) {
    if (func.apply(param)) {
      return Json.ok();
    }
    return Json.failed();
  }

  /**
   * @param success if success
   * @param message the message of the response
   * @param data response data
   */
  public static Json create(boolean success, String message, Object data) {
    return new Json()
            .data(data)
            .message(message)
            .success(success);
  }

  public static Json ok() {
    return create(true, BlogConstant.OPERATION_OK, null);
  }

  public static Json ok(String message, Object data) {
    return create(true, message, data);
  }

  public static Json ok(Object data) {
    return create(true, BlogConstant.OPERATION_OK, data);
  }

  public static Json ok(String message) {
    return create(true, message, null);
  }

  /**
   * default failed json
   */
  public static Json failed() {
    return create(false, BlogConstant.OPERATION_FAILED, null);
  }

  public static Json failed(Object data) {
    return create(false, BlogConstant.OPERATION_FAILED, data);
  }

  public static Json failed(String message) {
    return create(false, message, null);
  }

  public static Json failed(String message, Object data) {
    return create(false, message, data);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("data", data)
            .append("message", message)
            .append("success", success)
            .toString();
  }

}
