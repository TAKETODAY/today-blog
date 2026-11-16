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

package cn.taketoday.blog.maintenance.backup;

import cn.taketoday.blog.problem.Problem;
import infra.http.HttpStatus;

/**
 * 备份请求的问题定义。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public enum BackupProblems implements Problem {

  NOT_FOUND("backup.not_found", HttpStatus.NOT_FOUND),
  EXECUTOR_UNAVAILABLE("backup.executor_unavailable", HttpStatus.SERVICE_UNAVAILABLE);

  private final String code;

  private final HttpStatus status;

  BackupProblems(String code, HttpStatus status) {
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
