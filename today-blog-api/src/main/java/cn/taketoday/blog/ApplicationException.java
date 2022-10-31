/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.io.Serial;

import cn.taketoday.core.NoStackTraceRuntimeException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-08-05 13:37
 */
public class ApplicationException extends NoStackTraceRuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final HttpStatusCode statusCode;

  public ApplicationException() {
    this(null, null, HttpStatus.BAD_REQUEST);
  }

  public ApplicationException(String message) {
    this(message, null, HttpStatus.BAD_REQUEST);
  }

  public ApplicationException(Throwable cause) {
    this(null, cause, HttpStatus.BAD_REQUEST);
  }

  public ApplicationException(@Nullable String message, @Nullable Throwable cause, HttpStatusCode statusCode) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public HttpStatusCode getStatusCode() {
    return statusCode;
  }

  public static ApplicationException failed() {
    return new ApplicationException();
  }

  public static ApplicationException failed(HttpStatusCode statusCode) {
    return new ApplicationException(null, null, statusCode);
  }

  public static ApplicationException failed(String msg) {
    return new ApplicationException(msg);
  }

  public static ApplicationException failed(String msg, HttpStatusCode statusCode) {
    return new ApplicationException(msg, null, statusCode);
  }

  public static ApplicationException failed(Throwable cause) {
    return new ApplicationException(cause);
  }

  public static ApplicationException failed(String msg, Throwable cause) {
    return new ApplicationException(msg, cause, HttpStatus.BAD_REQUEST);
  }

}
