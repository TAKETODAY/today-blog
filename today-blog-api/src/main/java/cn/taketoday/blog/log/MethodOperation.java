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
package cn.taketoday.blog.log;

import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

import cn.taketoday.blog.model.User;

/**
 * 记录方法执行的上下文信息。
 * <p>
 * 该类用于封装方法调用时的关键数据，包括调用时间、客户端 IP 地址、
 * 方法调用对象以及当前登录的用户信息。
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-04 10:26
 */
class MethodOperation {

  /**
   * 方法被调用的时间点。
   */
  public final Instant invokeAt = Instant.now();

  /**
   * 发起请求的客户端 IP 地址。
   */
  public final String ip;

  /**
   * AOP 方法调用对象，包含目标方法及其参数等信息。
   */
  public final MethodInvocation invocation;

  /**
   * 当前登录的用户信息，如果未登录则为 {@code null}。
   */
  public final @Nullable User loginUser;

  /**
   * 构造一个新的方法操作记录。
   *
   * @param ip 客户端 IP 地址
   * @param invocation AOP 方法调用对象
   * @param loginUser 当前登录用户，可为 {@code null}
   */
  public MethodOperation(String ip, MethodInvocation invocation, @Nullable User loginUser) {
    this.ip = ip;
    this.loginUser = loginUser;
    this.invocation = invocation;
  }

}
