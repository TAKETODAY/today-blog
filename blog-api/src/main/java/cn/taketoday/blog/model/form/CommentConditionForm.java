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

package cn.taketoday.blog.model.form;

import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.Like;
import cn.taketoday.persistence.OrderBy;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/10/12 18:31
 */
@Data
@OrderBy("id DESC")
public class CommentConditionForm {

  @Nullable
  private CommentStatus status;

  @Like
  private String commenter;

  @Like
  private String commenterSite;

  private String email;

}
