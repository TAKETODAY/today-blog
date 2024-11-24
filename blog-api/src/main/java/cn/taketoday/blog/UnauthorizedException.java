/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.blog;

import java.io.Serial;

import infra.http.HttpStatus;
import infra.web.ResponseStatusException;

public class UnauthorizedException extends ResponseStatusException {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String UNAUTHORIZED = HttpStatus.UNAUTHORIZED.getReasonPhrase();

  public UnauthorizedException() {
    super(HttpStatus.UNAUTHORIZED, UNAUTHORIZED);
  }

  public UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(HttpStatus.UNAUTHORIZED, message, cause);
  }

  public UnauthorizedException(Throwable cause) {
    super(HttpStatus.UNAUTHORIZED, UNAUTHORIZED, cause);
  }

}