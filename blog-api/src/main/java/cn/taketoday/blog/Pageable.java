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

package cn.taketoday.blog;

import cn.taketoday.core.style.ToStringBuilder;

/**
 * 分页
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-25 01:11
 */
public interface Pageable {

  /**
   * Returns the page to be returned.
   *
   * @return the page to be returned.
   */
  int current();

  /**
   * Returns the number of items to be returned.
   *
   * @return the number of items of that page
   */
  int size();

  default int offset() {
    return (current() - 1) * size();
  }

  default int offset(int max) {
    return (current() - 1) * size(max);
  }

  default int size(int max) {
    return Math.min(size(), max);
  }

  static Simple of(int size, int current) {
    return new Simple(size, current);
  }

  record Simple(int size, int current) implements Pageable {

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof Simple simple))
        return false;
      return size == simple.size && current == simple.current;
    }

    @Override
    public String toString() {
      return ToStringBuilder.from(this)
              .append("size", size)
              .append("current", current)
              .toString();
    }
  }

}
