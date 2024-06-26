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

package cn.taketoday.blog.web.http;

import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.HttpResult;
import cn.taketoday.blog.web.Json;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.Page;
import cn.taketoday.web.ResponseStatusException;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.PATCH;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-22 20:38
 */
@RestController
@RequestMapping("/api/comments")
class CommentController {

  private final CommentConfig commentConfig;

  private final CommentService commentService;

  public CommentController(CommentConfig commentConfig, CommentService commentService) {
    this.commentConfig = commentConfig;
    this.commentService = commentService;
  }

  static class CommentFrom {

    @NotEmpty(message = "请输入评论内容")
    public String content;

    @NotNull
    public Long articleId;

    @Nullable
    public Long commentId;
  }

  /**
   * 评论文章
   *
   * @param loginInfo 登录用户信息
   * @param from 评论表单
   */
  @POST
  @RequestLimit
  @ResponseStatus(HttpStatus.CREATED)
  @Logging(title = "用户评论", content = "用户：[#{#loginInfo.loginUser.name}] " +
          "评论了文章:[#{@articleService.getById(#from.articleId).title}] 回复了:[#{#from.commentId}]")
  public void create(@RequiresUser LoginInfo loginInfo, @RequestBody @Valid CommentFrom from) {
    Comment comment = new Comment();
    comment.setUser(loginInfo.getLoginUser());
    comment.setUserId(loginInfo.getLoginUserId());
    comment.setContent(from.content);
    comment.setCommentId(from.commentId);
    comment.setArticleId(from.articleId);

    if (loginInfo.isBloggerLoggedIn()) {
      comment.setStatus(CommentStatus.CHECKED);
    }
    else {
      // check comment length
      if (comment.getContent().length() >= commentConfig.getContentLength()) {
        throw ErrorMessageException.failed("字数超出限制");
      }
      if (commentConfig.isCheck()) {
        // 检查是否需要审核
        comment.setStatus(CommentStatus.CHECKING);
      }
      else {
        comment.setStatus(CommentStatus.CHECKED);
      }
    }

    // comment.setContent(BlogUtils.stripXss(comment.getContent()));

    // save
    commentService.save(comment);
  }

  @GET("/articles/{id}")
  public HttpResult get(@PathVariable Long id, Pageable pageable) {
    return commentService.getByArticleId(id, pageable);
  }

  @RequiresBlogger
  @PATCH("/{id}/status")
  @Logging(title = "更新评论状态", content = "更新评论：[#{#id}]状态为：[#{#status}]")
  public void status(@PathVariable Long id, @RequestParam CommentStatus status) {
    commentService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @Logging(title = "删除评论", content = "删除评论：[#{#id}]")
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
    Page<Comment> byUser = commentService.getByUser(userInfo, pageable);
    assertFound(pageable, byUser.getTotalRows().intValue());
    return Pagination.from(byUser);
  }

  @SuppressWarnings("removal")
  protected void assertFound(Pageable pageable, int rowCount) {
    if (BlogUtils.notFound(pageable.pageNumber(), BlogUtils.pageCount(rowCount, pageable.pageSize()))) {
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
  @Logging(title = "更新评论", content = "更新评论：[#{#id}]")
  public Json put(@RequiresUser LoginInfo loginInfo, @PathVariable Long id, @RequestBody Comment comment) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (!loginInfo.isBloggerLoggedIn() && (loginInfo.getLoginUserId() != byId.getUserId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
    }

    comment.setId(id);
    commentService.updateById(comment);
    return Json.ok("更新成功");
  }

}
