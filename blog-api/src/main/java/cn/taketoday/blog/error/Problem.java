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

import java.net.URI;

import infra.http.HttpStatus;

/**
 * 定义可供客户端识别的问题类型，包括稳定错误码、默认 HTTP 状态和类型 URI。
 *
 * <p>领域可通过枚举实现此接口，也可使用 {@link #of(String, HttpStatus)} 创建
 * 不可变的默认实现。问题定义应保持稳定，不包含单次请求的数据或本地化文案；
 * 请求相关的消息参数由 {@link ProblemException} 携带，文案由
 * {@link ProblemResponses} 根据请求语言和错误码对应的资源模板解析。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ProblemException
 * @see ProblemResponses
 * @since 4.0 2026/11/16
 */
public interface Problem {

  /**
   * 创建使用默认类型 URI 规则的不可变问题定义。
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
   * <p>本地化消息资源使用 {@code <code>.title} 和 {@code <code>.detail} 作为键。
   *
   * @return 非空的业务错误码
   */
  String code();

  /**
   * 返回此问题类型默认使用的 HTTP 响应状态。
   *
   * @return 默认 HTTP 状态
   */
  HttpStatus status();

  /**
   * 返回标识此问题类型的稳定 URI。
   *
   * <p>默认返回 {@code urn:today-blog:problem:<code>}，其中 {@code <code>}
   * 为 {@link #code()} 的返回值。此 URI 用作类型标识，不表示可访问的文档地址；
   * 实现可按需覆盖此方法提供其他稳定 URI。
   *
   * @return 问题类型 URI
   * @throws IllegalArgumentException 如果错误码导致生成的 URI 不合法
   */
  default URI type() {
    return URI.create("urn:today-blog:problem:" + code());
  }

}
