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

package cn.taketoday.blog.event;

import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.web.LoginInfo;

/**
 * 当有新的评论时触发
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/3/5 22:13
 */
public class CommentCreatedEvent extends BlogEvent {

  private final Comment comment;

  private final LoginInfo loginInfo;

  public CommentCreatedEvent(Object source, Comment comment, LoginInfo loginInfo) {
    super(source);
    this.comment = comment;
    this.loginInfo = loginInfo;
  }

  public Comment getComment() {
    return comment;
  }

  public LoginInfo getLoginInfo() {
    return loginInfo;
  }
}
