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

package cn.taketoday.blog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.Id;
import cn.taketoday.persistence.Table;
import cn.taketoday.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY Time 2017 10 11--18:07
 */
@Setter
@Getter
@Table("t_comment")
public class Comment implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  private String content;

  private Long articleId;

  /** parent comment id */
  @JsonIgnore
  private Long commentId;

  private CommentStatus status;

  @JsonIgnore
  private Long userId;

  private LocalDateTime createAt;

  private LocalDateTime updateAt;

  @Nullable
  @Transient
  private User user;

  @Nullable
  @Transient
  private List<Comment> replies;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Comment comment))
      return false;
    return Objects.equals(id, comment.id)
            && status == comment.status
            && Objects.equals(content, comment.content)
            && Objects.equals(articleId, comment.articleId)
            && Objects.equals(commentId, comment.commentId)
            && Objects.equals(userId, comment.userId)
            && Objects.equals(createAt, comment.createAt)
            && Objects.equals(updateAt, comment.updateAt)
            && Objects.equals(user, comment.user)
            && Objects.equals(replies, comment.replies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, articleId, commentId, status, userId, createAt, updateAt, user, replies);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("content", content)
            .append("articleId", articleId)
            .append("commentId", commentId)
            .append("status", status)
            .append("userId", userId)
            .append("createAt", createAt)
            .append("updateAt", updateAt)
            .append("user", user)
            .append("replies", replies)
            .toString();
  }
}
