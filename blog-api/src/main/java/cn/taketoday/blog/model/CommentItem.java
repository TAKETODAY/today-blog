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

import java.time.LocalDateTime;
import java.util.List;

import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.Transient;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/10/8 22:02
 */
@Data
public class CommentItem {

  private Long id;

  private String content;

  /** 父级 ID */
  @JsonIgnore
  private Long parentId;

  @JsonIgnore
  private Long userId;

  /**
   * 评论者的 邮箱
   */
  private String email;

  /**
   * 评论者的名字
   */
  private String commenter;

  /**
   * 评论者的网站地址
   */
  @Nullable
  private String commenterSite;

  @Nullable
  private String commenterDesc;

  @Nullable
  @Transient
  private User user;

  @Nullable
  @Transient
  private List<Comment> replies;

  private LocalDateTime createAt;

  private LocalDateTime updateAt;

  public void setUser(@Nullable User user) {
    this.user = user;
    if (user != null) {
      commenterDesc = user.getIntroduce();
    }
    else {
      commenterDesc = null;
    }
  }

  public static CommentItem forComment(Comment comment) {
    CommentItem commentItem = new CommentItem();
    commentItem.setId(comment.getId());
    commentItem.setContent(comment.getContent());
    commentItem.setCreateAt(comment.getCreateAt());
    commentItem.setUpdateAt(comment.getUpdateAt());
    commentItem.setEmail(comment.getEmail());
    commentItem.setReplies(comment.getReplies());
    commentItem.setParentId(comment.getParentId());
    commentItem.setUserId(comment.getUserId());
    commentItem.setCommenter(comment.getCommenter());
    commentItem.setCommenterSite(comment.getCommenterSite());
    commentItem.setUser(comment.getUser());

    return commentItem;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("content", StringUtils.truncate(content, 10))
            .append("parent", parentId)
            .append("userId", userId)
            .append("createAt", createAt)
            .append("updateAt", updateAt)
            .append("user", user)
            .append("replies", replies)
            .toString();
  }

}
