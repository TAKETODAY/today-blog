/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/11 10:22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Interceptor(RequestLimitInterceptor.class)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequestLimit {

  /**
   * 允许访问的次数，默认值MAX_VALUE
   */
  int count() default Integer.MAX_VALUE;

  /**
   * 时间段
   */
  long timeout() default 1;

  TimeUnit timeUnit() default TimeUnit.SECONDS;

  String errorMessage() default Constant.DEFAULT_NONE;
}