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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * 日志注解，用于标记需要记录日志的方法或类。
 * <p>
 * 支持自定义日志标题和内容，内容中可使用 {@code result} 变量来定制可视化的结果。
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-10 19:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Logging {

  /**
   * 日志标题，等同于 {@link #title()}。
   *
   * @return 日志标题
   */
  @AliasFor(attribute = "title")
  String value() default Constant.BLANK;

  /**
   * 日志标题，等同于 {@link #value()}。
   *
   * @return 日志标题
   */
  @AliasFor(attribute = "value")
  String title() default Constant.BLANK;

  /**
   * 日志内容模板。
   * <p>
   * 支持使用 {@code result} 变量来引用方法执行结果，从而定制可视化的日志输出。
   * </p>
   *
   * @return 日志内容模板
   */
  String content() default Constant.DEFAULT_NONE;

}
