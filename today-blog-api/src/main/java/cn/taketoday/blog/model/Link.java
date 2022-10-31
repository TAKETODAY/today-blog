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
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 21:43
 */
@Setter
@Getter
public class Link implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /** 友情链接编号 */
  private long id;
  /** 友情链接名称 */
  private String name;
  /** 友情链接地址 */
  private String url;
  /** 友情链接头像 */
  private String image;
  /** 友情链接描述 */
  private String description;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("name", name)
            .append("url", url)
            .append("image", image)
            .append("description", description)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Link link))
      return false;
    return id == link.id
            && Objects.equals(url, link.url)
            && Objects.equals(name, link.name)
            && Objects.equals(image, link.image)
            && Objects.equals(description, link.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, url, image, description);
  }
}
