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

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.core.NoStackTraceRuntimeException;
import infra.http.HttpStatusCode;
import infra.lang.Constant;
import infra.web.HttpStatusProvider;

/**
 * 业务失败携带稳定类型、可选的请求级 detail 和允许公开的模板参数，在响应阶段才选择语言。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public class ProblemException extends NoStackTraceRuntimeException implements HttpStatusProvider {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Problem problem;

  private final Object[] arguments;

  private final @Nullable String detail;

  public ProblemException(Problem problem, Object... arguments) {
    this(problem, null, arguments);
  }

  protected ProblemException(Problem problem, @Nullable String detail, Object @Nullable [] arguments) {
    super(problem.code(), null);
    this.problem = problem;
    this.detail = detail;
    this.arguments = arguments == null ? Constant.EMPTY_OBJECTS : arguments.clone();
  }

  /**
   * @param problem 稳定的问题类型
   * @param arguments 问题资源模板参数
   * @return 问题异常
   */
  public static ProblemException of(Problem problem, Object... arguments) {
    return new ProblemException(problem, null, arguments);
  }

  /**
   * 创建携带请求级 detail 的问题异常。detail 优先作为消息键按请求语言解析；找不到消息键时保留原文，
   * 并覆盖问题默认 detail。
   *
   * @param problem 稳定的问题类型
   * @param detail 允许展示给客户端的本次请求说明
   * @param arguments 问题资源模板参数
   * @return 问题异常
   */
  public static ProblemException of(Problem problem, @Nullable String detail, Object... arguments) {
    return new ProblemException(problem, detail, arguments);
  }

  public Problem getProblem() {
    return problem;
  }

  public Object[] getArguments() {
    return arguments;
  }

  public @Nullable String getDetail() {
    return detail;
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
