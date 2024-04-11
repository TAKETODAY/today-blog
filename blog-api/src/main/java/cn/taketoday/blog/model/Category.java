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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.model;

import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.persistence.Id;
import cn.taketoday.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-17 14:59
 */
@Setter
@Getter
@Table("category")
public class Category {

  public static final int DEFAULT_ORDER = 128;

  @Id
  private String name;

  private Integer order;

  private Integer articleCount;

  private String description;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("order", order)
            .append("name", name)
            .append("articleCount", articleCount)
            .append("description", description)
            .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Category other) {
      return Objects.equals(this.order, other.order)
              && Objects.equals(this.name, other.name)
              && Objects.equals(this.description, other.description);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(order, name, description);
  }
}
