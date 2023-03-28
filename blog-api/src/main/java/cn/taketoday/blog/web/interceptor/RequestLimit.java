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

package cn.taketoday.blog.web.interceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Constant;
import cn.taketoday.web.annotation.Interceptor;

/**
 * 默认值：一秒钟请求一次
 * <p>
 * 单位时间内可以请求的次数, 超出部分将 {@link #errorMessage()} 返回给客户端,
 * 默认错误消息：{@link RequestLimitInterceptor#defaultErrorMessage}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestLimitInterceptor#setDefaultErrorMessage(String)
 * @since 4.0 2022/8/11 10:22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(RequestLimitInterceptor.class)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequestLimit {

  /**
   * 允许访问的次数，默认值 1 次
   */
  int count() default 1;

  /**
   * 时间段内能访问 {@link #count()} 次
   */
  long timeout() default 1;

  TimeUnit unit() default TimeUnit.SECONDS;

  String errorMessage() default Constant.DEFAULT_NONE;
}