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

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-20 15:32
 */
@Setter
@Getter
public class Atom implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private long updated;

  private LinkedList<Entry> entries = new LinkedList<>();

  public void addEntry(final Entry entry) {
    entries.add(entry);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Atom atom))
      return false;
    return updated == atom.updated && Objects.equals(entries, atom.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(updated, entries);
  }
}
