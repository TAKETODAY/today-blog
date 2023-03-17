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
import java.time.LocalDateTime;
import java.util.Objects;

import cn.taketoday.blog.model.enums.AttachmentType;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Id;
import cn.taketoday.jdbc.persistence.Table;
import cn.taketoday.jdbc.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table("attachment")
public class Attachment implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  /** 附件名 */
  private String name;

  /** CDN地址 */
  private String uri;

  /** 附件本地地址 */
  private String location;

  /** 附件类型 */
  private AttachmentType fileType;

  /** 附件大小 */
  private Long size;

  /** 是否同步 OSS */
  private Boolean sync;

  private LocalDateTime createAt;

  private LocalDateTime updateAt;

  @Transient
  public boolean isSynchronizedOSS() {
    return sync != null && sync;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Attachment that) {
      return Objects.equals(id, that.id)
              && fileType == that.fileType
              && Objects.equals(uri, that.uri)
              && Objects.equals(name, that.name)
              && Objects.equals(size, that.size)
              && Objects.equals(sync, that.sync)
              && Objects.equals(location, that.location);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, uri, location, fileType, size, sync);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("name", name)
            .append("uri", uri)
            .append("location", location)
            .append("fileType", fileType)
            .append("size", size)
            .append("sync", sync)
            .append("createAt", createAt)
            .append("updateAt", updateAt)
            .toString();
  }

}
