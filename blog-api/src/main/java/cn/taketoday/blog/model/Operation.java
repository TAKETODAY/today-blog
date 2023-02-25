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

package cn.taketoday.blog.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table("logger")
public class Operation implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private Long id;// created time
  /** 产生日志的ip */
  private String ip;
  /** 标题 */
  private String title;
  /** 内容 */
  private String content;
  // operation user
  private String user;
  private String type;
  private String result;

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("ip", ip)
            .append("title", title)
            .append("content", content)
            .append("user", user)
            .append("type", type)
            .append("result", result)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Operation operation))
      return false;
    return id == operation.id
            && Objects.equals(ip, operation.ip)
            && Objects.equals(title, operation.title)
            && Objects.equals(user, operation.user)
            && Objects.equals(type, operation.type)
            && Objects.equals(content, operation.content)
            && Objects.equals(result, operation.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, ip, title, content, user, type, result);
  }
}
