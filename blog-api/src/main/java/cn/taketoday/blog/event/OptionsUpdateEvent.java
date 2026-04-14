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

/**
 * 博客选项更新事件。
 * <p>
 * 当系统配置或博客选项发生更改时发布此事件，以便监听器可以做出相应的反应，
 * 例如刷新缓存或重新加载配置。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/1/12 15:51
 */
public class OptionsUpdateEvent extends BlogEvent {

  /**
   * 构造一个新的 {@code OptionsUpdateEvent} 实例。
   *
   * @param source 事件源对象
   */
  public OptionsUpdateEvent(Object source) {
    super(source);
  }

}
