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

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;

/**
 * 定义可供客户端识别的问题类型，包括稳定错误码和默认 HTTP 状态。
 *
 * <p>领域可通过枚举实现此接口，也可使用 {@link #of(String, HttpStatus)} 创建
 * 不可变的默认实现。问题定义应保持稳定，不包含单次请求的数据或本地化文案；
 * 请求相关的消息参数和可选的请求级 detail 由 {@link ProblemException} 携带，文案由
 * {@link ProblemResponses} 根据请求语言和错误码对应的资源模板解析。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProblemException
 * @see ProblemResponses
 * @since 4.0 2026/11/16
 */
public interface Problem {

  /**
   * 返回稳定的业务错误码，供客户端进行精确判断或按领域前缀分类。
   *
   * <p>错误码格式为 {@code 域[.子域...].错误标识}，使用点号 {@code .} 分隔域、
   * 子域和最终的错误标识，支持多级子域，不限定为两段。例如
   * {@code article.password_required} 中的域为 {@code article}，
   * {@code upgrade.package.not_found} 中的域为 {@code upgrade}、子域为 {@code package}。
   * 各段使用小写字母和数字，当前约定以下划线分隔段内单词。
   *
   * <p>按域分类时应使用以点号结尾的完整前缀，例如 {@code upgrade.} 或
   * {@code upgrade.package.}，避免匹配到名称相近但不属于该域的错误码。
   * 具体业务判断应使用完整错误码，不应仅凭域前缀推断处理动作。
   *
   * <p>本地化消息资源直接使用 {@code <code>} 作为正文键，使用 {@code <code>.title} 作为标题键。
   *
   * @return 非空的业务错误码
   */
  String code();

  /**
   * 返回此问题类型默认使用的 HTTP 响应状态。
   *
   * @return 默认 HTTP 状态
   */
  HttpStatusCode status();

  /**
   * 创建携带此问题类型和消息模板参数的问题异常。
   *
   * <p>返回的异常不包含请求级 detail，响应时使用错误码对应的资源模板生成正文。
   * 每次调用都会创建新的异常实例，问题定义本身保持不变。
   *
   * <p>注意：单个字符串参数会被 {@link #toException(String, Object...)} 匹配并当作
   * detail 处理；若字符串是模板参数而非 detail，应显式传入数组。
   *
   * @param arguments 问题资源模板参数，可为空
   * @return 携带此问题类型和参数的问题异常
   * @see ProblemException#getArguments()
   */
  default ProblemException toException(Object... arguments) {
    return new ProblemException(this, arguments);
  }

  /**
   * 创建携带请求级 detail 的问题异常。
   *
   * <p>detail 优先作为消息键按请求语言解析；找不到对应消息键时保留原文，
   * 并覆盖错误码对应的默认正文。detail 只应包含允许展示给客户端的内容。
   *
   * @param detail 允许展示给客户端的本次请求说明，可为 {@code null}
   * @param arguments 问题资源模板参数，可为空
   * @return 携带 detail 和参数的问题异常
   * @see ProblemException#of(Problem, String, Object...)
   */
  default ProblemException toException(@Nullable String detail, Object... arguments) {
    return new ProblemException(this, detail, arguments);
  }

  /**
   * 创建不可变的问题定义。
   *
   * <p>此方法不注册错误码或创建消息资源；调用方仍需为错误码配置相应的消息模板。
   *
   * @param code 稳定的业务错误码
   * @param status 默认 HTTP 状态
   * @return 不可变的问题定义
   * @throws NullPointerException 如果错误码或 HTTP 状态为 {@code null}
   * @throws IllegalArgumentException 如果错误码为空或仅包含空白字符
   * @see DefaultProblem
   */
  static Problem of(String code, HttpStatus status) {
    return new DefaultProblem(code, status);
  }

}
