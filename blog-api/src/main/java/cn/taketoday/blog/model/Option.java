/*
 * Copyright 2017 - 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

import infra.persistence.Column;
import infra.persistence.Id;
import infra.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-21 17:42
 */
@Setter
@Getter
@Table(name = "t_option")
public class Option {

  @Id
  private String name;

  private String value;

  @Column("public")
  private Boolean open;

  private String description;

  private Instant updateAt;

  private OptionValueType valueType;

  public Option() {
  }

  public Option(String name, @Nullable String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final Option option))
      return false;
    return Objects.equals(name, option.name)
            && Objects.equals(open, option.open)
            && Objects.equals(value, option.value)
            && Objects.equals(description, option.description)
            && Objects.equals(updateAt, option.updateAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value, open, description, updateAt);
  }

}
