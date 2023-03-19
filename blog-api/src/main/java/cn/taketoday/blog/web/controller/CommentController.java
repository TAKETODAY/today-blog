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

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.Json;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.Result;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import cn.taketoday.web.AccessForbiddenException;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.PATCH;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RestController;
import jakarta.validation.constraints.NotEmpty;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-22 20:38
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

  private final CommentConfig commentConfig;
  private final CommentService commentService;

  public CommentController(CommentConfig commentConfig, CommentService commentService) {
    this.commentConfig = commentConfig;
    this.commentService = commentService;
  }

  static class CommentFrom {

    @NotEmpty(message = "请输入评论内容")
    public String content;

    public Long articleId;

    public Long commentId;
  }

  /**
   * 评论文章
   *
   * @param loginInfo 登录用户信息
   * @param from 评论表单
   */
  @POST
  @RequestLimit(count = 1)
  @Logging(title = "用户评论", content = "用户：[${#loginInfo.loginUser.name}] " +
          "评论了文章:[${#from.articleId}] 回复了:[${#from.commentId}] 结果: [${#result}]")
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
        return Json.failed("字数超出限制");
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
      return Pagination.empty();
    }

    int commentPageSize = commentConfig.getListSize();
    int pageCount = BlogUtils.pageCount(totalRecord, commentPageSize);

    if (BlogUtils.notFound(page, pageCount)) {
      throw ErrorMessageException.failed("页数不存在");
    }

    return Pagination.ok(commentService.getByArticleId(id, page, commentPageSize))
            .size(commentPageSize)
            .total(totalRecord)
            .current(page)
            .applyNum();
  }

  @RequiresBlogger
  @PATCH("/{id}/status")
  @Logging(title = "更新评论状态", content = "更新评论：[${#id}]状态为：[${#status}]")
  public void status(@PathVariable Long id, @RequestParam CommentStatus status) {
    commentService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @Logging(title = "删除评论", content = "删除评论：[${#id}]")
  public Json delete(@RequiresUser LoginInfo loginInfo, @PathVariable Long id) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (loginInfo.getBlogger() == null && (loginInfo.getLoginUserId() != byId.getUserId())) {
      throw ErrorMessageException.failed("权限不足");
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
      throw ErrorMessageException.failed("分页不存在");
    }
  }

  // 分页

  @GET
  @RequiresBlogger
  public Pagination<Comment> get(Pageable pageable) {
    return commentService.pagination(pageable);
  }

  /**
   * 更新评论
   */
  @PUT("/{id}")
  @Logging(title = "更新评论", content = "更新评论：[${#id}]")
  public Json put(@RequiresUser LoginInfo loginInfo, @PathVariable Long id, @RequestBody Comment comment) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (!loginInfo.isBloggerLoggedIn() && (loginInfo.getLoginUserId() != byId.getUserId())) {
      throw new AccessForbiddenException("权限不足");
    }

    comment.setId(id);
    commentService.update(comment);
    return Json.ok("更新成功");
  }

}
