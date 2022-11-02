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

package cn.taketoday.blog.model;

import java.io.Serial;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @since 2017 10 11--18:07
 */
@Setter
@Getter
public class Article extends Post {
  @Serial
  private static final long serialVersionUID = 1L;

  /** category name */
  private String category;
  private String copyRight;
  private Set<Label> labels;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("category", category)
            .append("copyRight", copyRight)
            .append("labels", labels)
            .append("id", id)
            .append("image", image)
            .append("title", title)
            .append("pv", pv)
            .append("status", status)
            .append("summary", summary)
            .append("content", content)
            .append("markdown", markdown)
            .append("lastModify", lastModify)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Article article))
      return false;
    if (!super.equals(o)) {
      return false;
    }
    return Objects.equals(category, article.category)
            && Objects.equals(copyRight, article.copyRight)
            && Objects.equals(labels, article.labels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), category, copyRight, labels);
  }

}
