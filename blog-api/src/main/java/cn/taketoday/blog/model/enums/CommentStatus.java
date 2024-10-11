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
package cn.taketoday.blog.model.enums;

import cn.taketoday.lang.Enumerable;

/**
 * 评论状态
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-18 17:13
 */
public enum CommentStatus implements Enumerable<Integer> {

  CHECKED(0, "已审核"),

  CHECKING(1, "未审核"),

  RECYCLE(2, "回收站");

  private final int value;
  private final String desc;

  CommentStatus(int value, String desc) {
    this.value = value;
    this.desc = desc;
  }

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  public static CommentStatus valueOf(int value) {
    return switch (value) {
      case 0 -> CHECKED;
      case 1 -> CHECKING;
      default -> RECYCLE;
    };
  }
}
