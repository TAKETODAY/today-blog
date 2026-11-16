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

package cn.taketoday.blog.maintenance.upgrade;

import cn.taketoday.blog.error.Problem;
import infra.http.HttpStatus;

/**
 * 升级请求问题；任务记录中的执行失败不是 HTTP 请求错误。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public enum UpgradeProblems implements Problem {

  PACKAGE_NOT_FOUND("upgrade.package.not_found", HttpStatus.NOT_FOUND),
  PACKAGE_NAME_INVALID("upgrade.package.name_invalid", HttpStatus.BAD_REQUEST),
  PACKAGE_NOTE_INVALID("upgrade.package.note_invalid", HttpStatus.BAD_REQUEST),
  PACKAGE_SIZE_INVALID("upgrade.package.size_invalid", HttpStatus.BAD_REQUEST),
  PACKAGE_INVALID("upgrade.package.invalid", HttpStatus.BAD_REQUEST),
  PACKAGE_IN_USE("upgrade.package.in_use", HttpStatus.CONFLICT),
  PACKAGE_CONSUMED("upgrade.package.consumed", HttpStatus.CONFLICT),
  RECORD_NOT_FOUND("upgrade.record.not_found", HttpStatus.NOT_FOUND),
  MAPPING_INVALID("upgrade.mapping.invalid", HttpStatus.BAD_REQUEST),
  ENVIRONMENT_NOT_READY("upgrade.environment_not_ready", HttpStatus.CONFLICT),
  SPACE_INSUFFICIENT("upgrade.space_insufficient", HttpStatus.CONFLICT),
  IN_PROGRESS("upgrade.in_progress", HttpStatus.CONFLICT),
  START_FAILED("upgrade.start_failed", HttpStatus.INTERNAL_SERVER_ERROR),
  DEPLOYMENT_REQUIRED("upgrade.deployment_required", HttpStatus.CONFLICT);

  private final String code;

  private final HttpStatus status;

  UpgradeProblems(String code, HttpStatus status) {
    this.code = code;
    this.status = status;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public HttpStatus status() {
    return status;
  }
}
