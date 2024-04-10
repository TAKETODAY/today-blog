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

package cn.taketoday.blog.model.form;

import java.util.Objects;

import cn.taketoday.blog.model.enums.AttachmentType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Like;
import cn.taketoday.jdbc.persistence.OrderBy;
import cn.taketoday.lang.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY 2020/12/22 22:35
 */
@Getter
@Setter
@OrderBy(clause = "update_at DESC, create_at DESC")
public class AttachmentForm {

  @Nullable
  private AttachmentType fileType;

  @Like
  @Nullable
  private String name;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final AttachmentForm that))
      return false;
    return Objects.equals(name, that.name) && fileType == that.fileType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fileType);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("fileType", fileType)
            .append("name", name)
            .toString();
  }

}
