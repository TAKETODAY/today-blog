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

package cn.taketoday.blog.model.enums;

import cn.taketoday.lang.Enumerable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-18 15:56
 */
public enum UserStatus implements Enumerable<Integer> {

  NORMAL(0, "正常"),
  INACTIVE(1, "账号尚未激活"),
  LOCKED(2, "账号被锁"),
  RECYCLE(3, "账号被冻结");

  private final int value;
  private final String desc;

  //@off
  public static UserStatus valueOf(int code) {
    return switch (code) {
      case 0 -> NORMAL;
      case 1 -> INACTIVE;
      case 2 -> LOCKED;
      default -> RECYCLE;
    };
  }
  //@on

  UserStatus(int code, String desc) {
    this.value = code;
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

}
