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

package cn.taketoday.blog.web.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.web.annotation.Interceptor;

/**
 * 标记需要用户登录才能访问的方法或参数。
 * <p>
 * 当此注解应用于方法或参数时，系统将检查当前请求是否包含有效的用户登录信息。
 * 如果未登录，通常会触发 {@link LoginInterceptor} 进行拦截处理（如重定向到登录页或返回错误）。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see LoginInterceptor
 * @since 2019-04-21 08:21
 */
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(LoginInterceptor.class)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface RequiresUser {

}
