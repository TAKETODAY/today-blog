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
package cn.taketoday.blog.log;

import org.aopalliance.intercept.MethodInvocation;

import java.time.Instant;

import cn.taketoday.blog.model.User;
import cn.taketoday.lang.Nullable;

/**
 * 记录 方法执行的上下文
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-04 10:26
 */
public class MethodOperation {

  public final Instant invokeAt = Instant.now();

  @Nullable
  public final User loginUser;

  public final String ip;

  public final Object returnValue;

  public final Throwable throwable;

  public final MethodInvocation invocation;

  public final boolean afterThrowing;

  public MethodOperation(String ip, @Nullable Object returnValue, MethodInvocation invocation,
          @Nullable User loginUser, @Nullable Throwable throwable) {
    this.ip = ip;
    this.loginUser = loginUser;
    this.returnValue = returnValue;
    this.throwable = throwable;
    this.invocation = invocation;
    this.afterThrowing = throwable != null;
  }

}
