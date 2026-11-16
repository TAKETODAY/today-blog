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

package cn.taketoday.blog.error;

import java.util.Objects;

import infra.http.HttpStatus;

/**
 * 不可变的问题默认实现，使用接口约定生成问题类型 URI。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public record DefaultProblem(String code, HttpStatus status) implements Problem {

  public DefaultProblem {
    Objects.requireNonNull(code, "Problem code is required");
    Objects.requireNonNull(status, "Problem status is required");
    if (code.isBlank()) {
      throw new IllegalArgumentException("Problem code must not be blank");
    }
  }
}
