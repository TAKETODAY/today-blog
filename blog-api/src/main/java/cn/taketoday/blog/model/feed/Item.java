/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-20 15:30
 */
@Setter
@Getter
public class Item implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private long id;
  private long pubDate;
  private String image;
  private String title;
  private String summary;
  private String content;

  private Set<String> categories = new HashSet<>();

  public void addCategory(final String category) {
    categories.add(category);
  }

  public void addCategories(Collection<String> categories) {
    this.categories.addAll(categories);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("image", image)
            .append("title", title)
            .append("categories", categories)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Item item))
      return false;
    return id == item.id
            && pubDate == item.pubDate
            && Objects.equals(image, item.image)
            && Objects.equals(title, item.title)
            && Objects.equals(summary, item.summary)
            && Objects.equals(content, item.content)
            && Objects.equals(categories, item.categories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, pubDate, image, title, summary, content, categories);
  }
}
