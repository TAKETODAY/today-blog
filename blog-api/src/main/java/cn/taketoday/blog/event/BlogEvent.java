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

package cn.taketoday.blog.event;

import infra.context.ApplicationEvent;

/**
 * 博客事件基类，用于封装与博客业务相关的领域事件。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/1/12 15:52
 */
public abstract class BlogEvent extends ApplicationEvent {

  /**
   * 构造一个新的博客事件。
   *
   * @param source 事件源对象
   */
  public BlogEvent(Object source) {
    super(source);
  }

}
