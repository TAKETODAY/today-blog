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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Id;
import cn.taketoday.jdbc.persistence.Table;
import cn.taketoday.jdbc.persistence.Transient;
import cn.taketoday.lang.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY Time 2017 10 11--18:07
 */
@Setter
@Getter
@Table("comment")
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

  @JsonIgnore
  private Long lastModify;

  /** for checked mail */
  private Boolean sendMail;

  @Nullable
  @Transient
  private User user;

  @Nullable
  @Transient
  private List<Comment> replies;

  public boolean getSendMail() {
    return sendMail != null && sendMail;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof Comment comment) {
      return Objects.equals(comment.id, id)
              && Objects.equals(comment.userId, userId)
              && Objects.equals(comment.articleId, articleId)
              && Objects.equals(comment.commentId, commentId)
              && Objects.equals(comment.content, content);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, articleId, commentId,
            status, userId, user, lastModify, replies, sendMail);
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
            .append("user", user)
            .append("lastModify", lastModify)
            .append("replies", replies)
            .append("sendMail", sendMail)
            .toString();
  }

}
