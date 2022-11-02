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

package cn.taketoday.blog.model.feed;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-20 15:29
 */
@Getter
@Setter
public class Rss implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private long lastBuildDate;

  private LinkedList<Item> items = new LinkedList<>();

  public void addItem(Item item) {
    items.add(item);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("lastBuildDate", lastBuildDate)
            .append("items", items)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Rss rss))
      return false;
    return lastBuildDate == rss.lastBuildDate && Objects.equals(items, rss.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastBuildDate, items);
  }
}
