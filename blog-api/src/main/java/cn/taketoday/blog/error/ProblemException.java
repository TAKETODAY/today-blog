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

import org.jspecify.annotations.Nullable;

import java.util.Objects;

import infra.core.NoStackTraceRuntimeException;
import infra.http.HttpStatusCode;
import infra.web.HttpStatusProvider;

/**
 * 业务失败携带稳定类型和允许公开的模板参数，在响应阶段才选择语言。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public final class ProblemException extends NoStackTraceRuntimeException implements HttpStatusProvider {

  private final Problem problem;

  private final Object[] arguments;

  public ProblemException(Problem problem, Object... arguments) {
    super(Objects.requireNonNull(problem).code(), null);
    this.problem = problem;
    this.arguments = arguments.clone();
  }

  public Problem getProblem() {
    return problem;
  }

  public Object[] getArguments() {
    return arguments.clone();
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return problem.status();
  }

  public static void notNull(@Nullable Object value, Problem problem, Object... arguments) {
    if (value == null) {
      throw new ProblemException(problem, arguments);
    }
  }
}
