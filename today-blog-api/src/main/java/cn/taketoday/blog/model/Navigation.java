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
 * @since 2018-10-21 14:31
 */
@Setter
@Getter
public class Navigation implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /** 编号 自增 */
  private long id;
  /** 菜单名称 */
  private String name;
  /** 菜单路径 */
  private String url;
  /** 排序编号 */
  private int order;
  /** 图标，可选，部分主题可显示 */
  private String icon;
  /** 打开方式 */
  private String target;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("name", name)
            .append("url", url)
            .append("order", order)
            .append("icon", icon)
            .append("target", target)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Navigation that))
      return false;
    return id == that.id
            && order == that.order
            && Objects.equals(name, that.name)
            && Objects.equals(url, that.url)
            && Objects.equals(icon, that.icon)
            && Objects.equals(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, url, order, icon, target);
  }
}
