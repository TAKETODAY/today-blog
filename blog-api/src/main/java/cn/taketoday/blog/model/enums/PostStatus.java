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

package cn.taketoday.blog.model.enums;

import cn.taketoday.lang.Enumerable;

/**
 * 文章状态
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-17 21:57
 */
public enum PostStatus implements Enumerable<Integer> {

  /** 已发布 */
  PUBLISHED(0, "已发布"),
  /** 草稿 */
  DRAFT(1, "草稿"),
  /** 回收站 */
  RECYCLE(2, "回收站");

  private final int value;
  private final String desc;

  PostStatus(int value, String desc) {
    this.value = value;
    this.desc = desc;
  }

  public static PostStatus valueOf(int code) {
    return switch (code) {
      case 0 -> PUBLISHED;
      case 1 -> DRAFT;
      default -> RECYCLE;
    };
  }

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public String getDescription() {
    return desc;
  }

}
