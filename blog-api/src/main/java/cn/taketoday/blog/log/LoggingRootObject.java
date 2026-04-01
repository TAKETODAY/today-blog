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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import cn.taketoday.blog.model.User;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2026/3/31 22:26
 */
public class LoggingRootObject {

  final MethodOperation operation;

  final Method method;

  final @Nullable Object @Nullable [] args;

  final Object target;

  final Class<?> targetClass;

  final @Nullable Object result;

  LoggingRootObject(MethodOperation operation, Method method, @Nullable Object @Nullable [] args,
          Object target, @Nullable Object result) {
    this.operation = operation;
    this.method = method;
    this.args = args;
    this.target = target;
    this.targetClass = target.getClass();
    this.result = result;
  }

  public Method getMethod() {
    return this.method;
  }

  public String getMethodName() {
    return this.method.getName();
  }

  public @Nullable Object @Nullable [] getArgs() {
    return args;
  }

  public Object getTarget() {
    return this.target;
  }

  public Object getThis() {
    return target;
  }

  public MethodOperation getOperation() {
    return operation;
  }

  public Class<?> getTargetClass() {
    return targetClass;
  }

  public @Nullable Object getResult() {
    return result;
  }

  public @Nullable String getEmail() {
    if (operation.loginUser != null) {
      return operation.loginUser.getEmail();
    }
    return null;
  }

  public @Nullable User getUser() {
    if (operation.loginUser != null) {
      return operation.loginUser;
    }
    return null;
  }

}
