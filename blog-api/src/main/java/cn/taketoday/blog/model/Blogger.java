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

package cn.taketoday.blog.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-07 22:00
 */
@Setter
@Getter
@NoArgsConstructor
public class Blogger implements Serializable {
  public static final String AttributeKey = "bloggerInfo";

  @Serial
  private static final long serialVersionUID = 1L;

  private int id = 0;
  private int age;
  private String sex;
  private String name;
  private String email;
  private String passwd;
  private String introduce;
  private String image;
  private String address;

  public Blogger(Integer id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("age", age)
            .append("sex", sex)
            .append("name", name)
            .append("email", email)
            .append("passwd", passwd)
            .append("introduce", introduce)
            .append("image", image)
            .append("address", address)
            .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof final Blogger other) {
      return Objects.equals(age, other.age)
              && Objects.equals(sex, other.sex)
              && Objects.equals(name, other.name)
              && Objects.equals(email, other.email)
              && Objects.equals(image, other.image)
              && Objects.equals(address, other.address)
              && Objects.equals(introduce, other.introduce);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, age, sex, name, email, passwd, introduce, image, address);
  }

  /**
   * Bind this instance to AttributeAccessor
   *
   * @param accessor session or request
   */
  public void bindTo(AttributeAccessor accessor) {
    accessor.setAttribute(AttributeKey, this);
  }

  // Static

  @Nullable
  public static Blogger find(AttributeAccessor accessor) {
    Object attribute = accessor.getAttribute(AttributeKey);
    if (attribute instanceof Blogger blogger) {
      return blogger;
    }
    return null;
  }

  public static Blogger obtain(AttributeAccessor accessor) {
    Blogger blogger = find(accessor);
    if (blogger == null) {
      throw new UnauthorizedException();
    }
    return blogger;
  }

  public static void unbind(AttributeAccessor accessor) {
    accessor.removeAttribute(AttributeKey);
  }

  /**
   * Is blogger logged in
   */
  public static boolean isPresent(@Nullable AttributeAccessor accessor) {
    return accessor != null && find(accessor) != null;
  }

}
