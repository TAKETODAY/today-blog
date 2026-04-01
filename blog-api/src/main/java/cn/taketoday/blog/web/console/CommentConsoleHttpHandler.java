/*
 * Copyright 2017 - 2025 the original author or authors.
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

package cn.taketoday.blog.web.console;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.form.CommentConditionForm;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.web.annotation.GET;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/3/5 22:29
 */
@RequiresBlogger
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/console/comments")
class CommentConsoleHttpHandler {

  private final CommentService commentService;

  /**
   * 后台评论列表
   */
  @GET
  public Pagination<Comment> listComments(CommentConditionForm form, Pageable pageable) {
    return commentService.pagination(form, pageable);
  }

  /**
   * 更新评论
   */
  @PUT("/{id}")
  @Logging(title = "更新评论", content = "更新评论：[#{#id}]")
  public void update(@PathVariable Long id, @RequestBody Comment comment) {
    Comment byId = commentService.obtainById(id);

    comment.setId(id);
    commentService.updateById(comment);
  }

}
