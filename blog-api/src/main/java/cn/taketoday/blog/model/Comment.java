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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.util.StringUtils;
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

  private CommentStatus status;

  private Long articleId;

  /**
   * 文章标题
   *
   * @since 3.2
   */
  private String articleTitle;

  /**
   * 评论者的 邮箱
   *
   * @since 3.2
   */
  private String email;

  /**
   * 评论者的名字
   *
   * @since 3.2
   */
  private String commenter;

  /**
   * 评论者的网站地址
   *
   * @since 3.2
   */
  @Nullable
  private String commenterSite;

  /** parent comment id */
  @JsonIgnore
  @Nullable
  private Long parentId;

  @Nullable
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
            && Objects.equals(parentId, comment.parentId)
            && Objects.equals(userId, comment.userId)
            && Objects.equals(createAt, comment.createAt)
            && Objects.equals(updateAt, comment.updateAt)
            && Objects.equals(user, comment.user)
            && Objects.equals(replies, comment.replies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content, articleId, parentId, status, userId, createAt, updateAt, user, replies);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("content", StringUtils.truncate(content, 10))
            .append("articleId", articleId)
            .append("parent", parentId)
            .append("status", status)
            .append("userId", userId)
            .append("createAt", createAt)
            .append("updateAt", updateAt)
            .append("user", user)
            .append("replies", replies)
            .toString();
  }
}
