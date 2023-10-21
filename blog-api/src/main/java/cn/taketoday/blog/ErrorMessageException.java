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

import java.util.function.Supplier;

import cn.taketoday.core.NoStackTraceRuntimeException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.HttpStatusCodeProvider;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * No StackTrace Exception
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/19 22:01
 */
public class ErrorMessageException extends NoStackTraceRuntimeException implements HttpStatusCodeProvider {

  private final HttpStatus status;

  public ErrorMessageException(@Nullable String msg) {
    this(msg, null, HttpStatus.BAD_REQUEST);
  }

  public ErrorMessageException(@Nullable String msg, @Nullable Throwable cause, HttpStatus status) {
    super(msg, cause);
    Assert.notNull(status, "http status is required");
    this.status = status;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return status;
  }

  public static ErrorMessageException failed(String message) {
    return new ErrorMessageException(message);
  }

  public static ErrorMessageException failed(String message, HttpStatus status) {
    return new ErrorMessageException(message, null, status);
  }

  public static void notNull(Object obj, String message) {
    if (obj == null) {
      throw ErrorMessageException.failed(message, HttpStatus.NOT_FOUND);
    }
  }

  public static void notNull(Object obj, Supplier<String> supplier) {
    if (obj == null) {
      throw new ErrorMessageException(supplier.get());
    }
  }

}
