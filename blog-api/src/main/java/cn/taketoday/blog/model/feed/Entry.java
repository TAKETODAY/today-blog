/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-2018, b3log.org & hacpai.com
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.blog.model.feed;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-20 15:34
 */
@Setter
@Getter
public final class Entry implements Serializable {
  private static final long serialVersionUID = 1L;

  private long id;
  private String title;
  private long updated;
  private String image;
  private long published;
  private String summary;
  private String content;

  private Set<String> categories = new HashSet<>();

  public void addCategory(String category) {
    categories.add(category);
  }

  public void addCategories(Collection<String> categories) {
    categories.addAll(categories);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Entry))
      return false;
    final Entry entry = (Entry) o;
    return id == entry.id && updated == entry.updated && published == entry.published && Objects
            .equals(title, entry.title) && Objects.equals(image, entry.image) && Objects
            .equals(summary, entry.summary) && Objects.equals(content, entry.content) && Objects
            .equals(categories, entry.categories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, updated, image, published, summary, content, categories);
  }
}
