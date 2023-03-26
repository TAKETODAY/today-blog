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

package cn.taketoday.blog.model.form;

import java.util.Map;
import java.util.Objects;

import cn.taketoday.blog.model.enums.OrderBy;
import cn.taketoday.blog.model.enums.PostStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY 2020/12/20 22:42
 */
@Getter
@Setter
public class SearchForm {

  private String title;
  private String content;
  private String category;
  private PostStatus status;

  private Map<String, OrderBy> sort;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final SearchForm that))
      return false;
    return status == that.status
            && Objects.equals(title, that.title)
            && Objects.equals(content, that.content)
            && Objects.equals(category, that.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, content, category, status);
  }
}
