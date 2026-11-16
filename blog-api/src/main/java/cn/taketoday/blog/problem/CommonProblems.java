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

package cn.taketoday.blog.problem;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;

/**
 * 通用请求与系统问题；领域业务问题由对应枚举定义。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public enum CommonProblems implements Problem {

  INVALID_REQUEST("request.invalid", HttpStatus.BAD_REQUEST),
  VALIDATION_FAILED("request.validation_failed", HttpStatus.BAD_REQUEST),
  PARAMETER_MISSING("request.parameter_missing", HttpStatus.BAD_REQUEST),
  METHOD_NOT_ALLOWED("request.method_not_allowed", HttpStatus.METHOD_NOT_ALLOWED),
  NOT_ACCEPTABLE("request.not_acceptable", HttpStatus.NOT_ACCEPTABLE),
  PAYLOAD_TOO_LARGE("request.payload_too_large", HttpStatus.PAYLOAD_TOO_LARGE),
  UNSUPPORTED_MEDIA_TYPE("request.unsupported_media_type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  RATE_LIMIT_EXCEEDED("request.rate_limit_exceeded", HttpStatus.TOO_MANY_REQUESTS),
  PAGE_INVALID("request.page_invalid", HttpStatus.BAD_REQUEST),
  PAGE_SIZE_INVALID("request.page_size_invalid", HttpStatus.BAD_REQUEST),
  PAGE_SIZE_EXCEEDED("request.page_size_exceeded", HttpStatus.BAD_REQUEST),
  DATE_RANGE_INVALID("request.date_range_invalid", HttpStatus.BAD_REQUEST),
  ILLEGAL_ARGUMENT("request.illegal_argument", HttpStatus.BAD_REQUEST),

  AUTH_REQUIRED("auth.required", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED("auth.access_denied", HttpStatus.FORBIDDEN),
  INVALID_CREDENTIALS("auth.invalid_credentials", HttpStatus.UNAUTHORIZED),

  CONFLICT("resource.conflict", HttpStatus.CONFLICT),
  NOT_FOUND("resource.not_found", HttpStatus.NOT_FOUND),
  NO_CHANGES("resource.no_changes", HttpStatus.BAD_REQUEST),

  INTERNAL_ERROR("system.internal_error", HttpStatus.INTERNAL_SERVER_ERROR),
  SERVICE_UNAVAILABLE("system.service_unavailable", HttpStatus.SERVICE_UNAVAILABLE);

  private final String code;

  private final HttpStatusCode status;

  CommonProblems(String code, HttpStatusCode status) {
    this.code = code;
    this.status = status;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public HttpStatusCode status() {
    return status;
  }

  /**
   * 仅用于尚未携带具体问题类型的旧异常，保留其真实 HTTP 状态。
   */
  public static Problem forStatus(HttpStatusCode status) {
    return switch (status.value()) {
      case 401 -> AUTH_REQUIRED;
      case 403 -> ACCESS_DENIED;
      case 404 -> NOT_FOUND;
      case 405 -> METHOD_NOT_ALLOWED;
      case 406 -> NOT_ACCEPTABLE;
      case 409 -> CONFLICT;
      case 413 -> PAYLOAD_TOO_LARGE;
      case 415 -> UNSUPPORTED_MEDIA_TYPE;
      case 429 -> RATE_LIMIT_EXCEEDED;
      case 503 -> SERVICE_UNAVAILABLE;
      default -> status.value() >= 500 ? INTERNAL_ERROR : INVALID_REQUEST;
    };
  }
}
