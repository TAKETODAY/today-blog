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

import java.io.Serial;
import java.time.Instant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/10/13 17:40
 */
public abstract class BasicModel extends BasicGeneratedIdModel {

  @Serial
  private static final long serialVersionUID = 1L;

  protected Instant createAt;

  protected Instant updateAt;

  public void setCreateAt(Instant createAt) {
    this.createAt = createAt;
  }

  public void setUpdateAt(Instant updateAt) {
    this.updateAt = updateAt;
  }

  public Instant getCreateAt() {
    return createAt;
  }

  public Instant getUpdateAt() {
    return updateAt;
  }

}
