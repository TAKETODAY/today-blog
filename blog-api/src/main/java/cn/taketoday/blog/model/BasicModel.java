/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import cn.taketoday.persistence.Id;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/10/13 17:40
 */
public class BasicModel implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  protected Long id;

  protected Instant createAt;

  protected Instant updateAt;

  public void setId(Long id) {
    this.id = id;
  }

  public void setCreateAt(Instant createAt) {
    this.createAt = createAt;
  }

  public void setUpdateAt(Instant updateAt) {
    this.updateAt = updateAt;
  }

  public Long getId() {
    return id;
  }

  public Instant getCreateAt() {
    return createAt;
  }

  public Instant getUpdateAt() {
    return updateAt;
  }
}
