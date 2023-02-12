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

package cn.taketoday.blog.web.controller;

import cn.taketoday.blog.ApplicationException;
import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.aspect.Logging;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.utils.BlogUtils;
import cn.taketoday.blog.utils.Json;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.blog.utils.Result;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-22 20:38
 */
@Controller
@RequestMapping("/api/comments")
public class CommentController {

  private final CommentConfig commentConfig;
  private final CommentService commentService;

  public CommentController(CommentConfig commentConfig, CommentService commentService) {
    this.commentConfig = commentConfig;
    this.commentService = commentService;
  }

  @Setter
  private static class CommentFrom {
    @NotEmpty
            //        @NotBlank
            (message = "请输入评论内容")
    private String content;

    @Min(10000)
    private long articleId;

    private long commentId;
  }

  /**
   * @param loginInfo logged in user info
   * @param from comment detail
   */
  @POST
  @Logging(title = "用户评论", content = "用户：[${userInfo.name}] 评论了文章:[${from.articleId}] 回复了:[${from.commentId}] 结果: [${result}]")
  @RequestLimit(count = 1)
  public Json post(@RequiresUser LoginInfo loginInfo, @RequestBody CommentFrom from) {
    Comment comment = new Comment();
    comment.setUser(loginInfo.getLoginUser());
    comment.setUserId(loginInfo.getLoginUser().getId());
    comment.setContent(from.content);
    comment.setCommentId(from.commentId);
    comment.setArticleId(from.articleId);

    if (loginInfo.isBloggerLoggedIn()) {
      comment.setStatus(CommentStatus.CHECKED);
    }
    else {
      // check comment length
      if (comment.getContent().length() >= commentConfig.getContentLength()) {
        return Json.badRequest(BlogConstant.OVER_CONTENT);
      }
      if (commentConfig.isCheck()) {
        comment.setStatus(CommentStatus.CHECKING);
      }
      else {
        comment.setStatus(CommentStatus.CHECKED);
      }
    }
    comment.setId(System.currentTimeMillis());

    // comment.setContent(BlogUtils.stripXss(comment.getContent()));

    // save
    commentService.save(comment);
    return Json.ok(BlogConstant.COMMENT_SUCCESS);
  }

  @GET("/articles/{id}")
  public Result get(@PathVariable Long id, @RequestParam(defaultValue = "1") int page) {

    int totalRecord = commentService.countByArticleId(id);
    if (totalRecord <= 0) {
      return Pagination.ok();
    }

    int commentPageSize = commentConfig.getListSize();
    int pageCount = BlogUtils.pageCount(totalRecord, commentPageSize);

    if (BlogUtils.notFound(page, pageCount)) {
      throw ApplicationException.failed("页数不存在");
    }

    return Pagination.ok(commentService.getByArticleId(id, page, commentPageSize))
            .size(commentPageSize)
            .all(totalRecord)
            .current(page)
            .applyNum();
  }

  @PUT("/{id}/status")
  @RequiresBlogger
  @Logging(title = "更新评论状态", content = "更新评论：[${id}]状态为：[${CommentStatus.valueOf(code)}]")
  public Json status(@PathVariable Long id, @RequestParam(required = true) int code) {

    commentService.updateStatusById(CommentStatus.valueOf(code), id);
    return Json.ok();
  }

  @DELETE("/{id}")
  @Logging(title = "删除评论", content = "删除评论：[${id}]")
  public Json delete(@RequiresUser LoginInfo loginInfo, @PathVariable Long id) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (loginInfo.getBlogger() == null && (loginInfo.getLoginUserId() != byId.getUserId())) {
      throw ApplicationException.failed("权限不足");
    }

    commentService.delete(id);
    return Json.ok("删除成功");
  }

  // -----------------------------

  @GET("/users")
  public Pagination<Comment> getByUser(User userInfo, Pageable pageable) {
    int rowCount = commentService.countByUser(userInfo);
    assertFound(pageable, rowCount);
    return Pagination.ok(commentService.getByUser(userInfo, pageable), rowCount, pageable);
  }

  protected void assertFound(Pageable pageable, int rowCount) {
    if (BlogUtils.notFound(pageable.getCurrent(), BlogUtils.pageCount(rowCount, pageable.getSize()))) {
      throw ApplicationException.failed("分页不存在");
    }
  }

  // 分页

  @GET
  @RequiresBlogger
  public Pagination<Comment> get(Pageable pageable) {
    return commentService.pagination(pageable);
  }

  //

  @PUT("/{id}")
  @Logging(title = "更新评论", content = "更新评论：[${id}]")
  public Json put(@RequiresUser LoginInfo loginInfo, @PathVariable Long id, @RequestBody Comment comment) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (!loginInfo.isBloggerLoggedIn() && (loginInfo.getLoginUserId() != byId.getUserId())) {
      throw ApplicationException.failed("权限不足");
    }

    comment.setId(id);
    commentService.update(comment);
    return Json.ok("更新成功");
  }

}
