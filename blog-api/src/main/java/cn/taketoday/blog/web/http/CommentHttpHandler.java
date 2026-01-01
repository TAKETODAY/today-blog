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

package cn.taketoday.blog.web.http;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.event.CommentCreatedEvent;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.CommentItem;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.model.form.CommentConditionForm;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequestLimit;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import infra.context.ApplicationEventPublisher;
import infra.http.HttpStatus;
import infra.persistence.Page;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.PATCH;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.server.ResponseStatusException;
import infra.web.util.UriBuilder;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-22 20:38
 */
@RestController
@RequestMapping("/api/comments")
class CommentHttpHandler {

  private final CommentConfig commentConfig;

  private final CommentService commentService;

  private final ArticleService articleService;

  private final ApplicationEventPublisher eventPublisher;

  public CommentHttpHandler(CommentConfig commentConfig, CommentService commentService,
          ArticleService articleService, ApplicationEventPublisher eventPublisher) {
    this.commentConfig = commentConfig;
    this.commentService = commentService;
    this.articleService = articleService;
    this.eventPublisher = eventPublisher;
  }

  static class CommentFrom {

    @NotEmpty(message = "请输入评论内容")
    public String content;

    @NotNull
    public Long articleId;

    @Nullable
    public Long commentId;

    /**
     * 评论者的邮箱
     */
    @Email
    public String email;

    /**
     * 评论者的昵称/名字
     */
    @NotEmpty(message = "请输入昵称")
    public String commenter;

    /**
     * 评论者的网站地址
     */
    @Nullable
    public String commenterSite;

  }

  /**
   * 评论文章
   * <p>
   * 十秒钟评论一次
   *
   * @param loginInfo 登录用户信息
   * @param from 评论表单
   */
  @POST
  @RequestLimit(timeout = 10)
  @ResponseStatus(HttpStatus.CREATED)
  @Logging(title = "用户评论", content = "用户：[#{#from.commenter}] " +
          "评论了文章:[#{@articleService.getById(#from.articleId)?.title}] 回复了:[#{#from.commentId}]")
  public void create(LoginInfo loginInfo, @RequestBody @Valid CommentFrom from) {
    Article article = articleService.obtainById(from.articleId);

    Comment comment = new Comment();
    comment.setContent(from.content);
    comment.setParentId(from.commentId);
    comment.setArticleId(from.articleId);
    comment.setArticleTitle(article.getTitle());

    comment.setEmail(from.email);
    comment.setCommenter(from.commenter);
    if (StringUtils.hasText(from.commenterSite)) {
      UriComponents uriComponents = UriComponentsBuilder.forURIString(from.commenterSite).build();
      String host = uriComponents.getHost();
      if (StringUtils.hasText(host)) {
        UriComponents commenterSite = UriBuilder.forUriComponents()
                .scheme(uriComponents.getScheme() == null ? "https" : uriComponents.getScheme())
                .host(host).build();

        comment.setCommenterSite(commenterSite.toUriString());
      }
    }

    if (loginInfo.isLoggedIn()) {
      comment.setUserId(loginInfo.getLoginUserId());
    }

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

    commentService.create(comment);
    eventPublisher.publishEvent(new CommentCreatedEvent(this, comment));
  }

  /**
   * @since 3.2
   */
  @RequestLimit
  @GET(params = "articleId")
  public List<CommentItem> getArticleComments(long articleId) {
    return commentService.fetchByArticleId(articleId);
  }

  @RequiresBlogger
  @PATCH("/{id}/status")
  @Logging(title = "更新评论状态", content = "更新评论：[#{#id}]状态为：[#{#status}]")
  public void status(@PathVariable Long id, @RequestParam CommentStatus status) {
    commentService.updateStatusById(status, id);
  }

  /**
   * 更新评论状态
   *
   * @since 3.2
   */
  @RequiresBlogger
  @PUT(path = "/{id}", params = "status")
  @Logging(title = "更新评论状态", content = "更新评论：[#{#id}]状态为：[#{#status}]")
  public void updateStatus(@PathVariable Long id, @RequestParam CommentStatus status) {
    commentService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @Logging(title = "删除评论", content = "删除评论：[#{#id}]")
  public void delete(@RequiresUser LoginInfo loginInfo, @PathVariable Long id) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (!loginInfo.isBloggerLoggedIn() && !Objects.equals(loginInfo.getLoginUserId(), byId.getUserId())) {
      throw ErrorMessageException.failed("权限不足");
    }

    commentService.delete(id);
  }

  // -----------------------------

  /**
   * 获取用户自己的评论
   */
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

  @Deprecated(forRemoval = true)
  @GET
  @RequiresBlogger
  public Pagination<Comment> list(CommentConditionForm form, Pageable pageable) {
    return commentService.pagination(form, pageable);
  }

  /**
   * 更新评论
   */
  @Deprecated
  @PUT("/{id}")
  @Logging(title = "更新评论", content = "更新评论：[#{#id}]")
  public void update(@RequiresUser LoginInfo loginInfo, @PathVariable Long id, @RequestBody Comment comment) {
    Comment byId = commentService.obtainById(id);

    // not blogger
    if (!loginInfo.isBloggerLoggedIn() && !Objects.equals(loginInfo.getLoginUserId(), byId.getUserId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
    }

    comment.setId(id);
    commentService.updateById(comment);
  }

}
