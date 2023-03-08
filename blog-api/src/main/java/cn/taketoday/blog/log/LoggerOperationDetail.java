/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.blog.log;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.blog.MethodOperation;
import cn.taketoday.blog.model.User;
import cn.taketoday.lang.Nullable;
import lombok.Getter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-31 09:18
 */
@Getter
public final class LoggerOperationDetail implements MethodOperation {

  private final long id;

  @Nullable
  private final User user;

  private final String ip;
  private final Object result;
  private final MethodInvocation invocation;

  private final boolean afterThrowing;

  public LoggerOperationDetail(String ip, Object result, MethodInvocation invocation, @Nullable User user) {
    this.ip = ip;
    this.user = user;
    this.result = result;
    this.afterThrowing = false;
    this.invocation = invocation;
    this.id = System.currentTimeMillis();
  }

  public LoggerOperationDetail(String ip, Object result, MethodInvocation invocation, @Nullable User user, boolean afterThrowing) {
    this.ip = ip;
    this.user = user;
    this.result = result;
    this.invocation = invocation;
    this.afterThrowing = afterThrowing;
    this.id = System.currentTimeMillis();
  }

  @Override
  public long getTimestamp() {
    return id;
  }

  @Nullable
  @Override
  public User getUser() {
    return user;
  }

  @Override
  public Object getResult() {
    return result;
  }

  @Override
  public MethodInvocation getInvocation() {
    return invocation;
  }

}
