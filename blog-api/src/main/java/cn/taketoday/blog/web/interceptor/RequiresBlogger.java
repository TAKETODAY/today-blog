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
 * 标记需要博主权限才能访问的资源。
 * <p>
 * 该注解用于类或方法级别，表示只有经过认证且拥有博主角色的用户才能访问被标记的资源。
 * 当请求到达时，{@link BloggerInterceptor} 拦截器将检查当前用户是否具有相应的权限。
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BloggerInterceptor
 * @since 4.0 2022/8/11 08:52
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Interceptor(BloggerInterceptor.class)
public @interface RequiresBlogger {

}
