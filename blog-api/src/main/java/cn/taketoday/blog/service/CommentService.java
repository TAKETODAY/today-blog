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

package cn.taketoday.blog.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.CommentItem;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.model.form.CommentConditionForm;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.EntityManager;
import cn.taketoday.persistence.OrderBy;
import cn.taketoday.persistence.Page;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.util.CollectionUtils;
import lombok.RequiredArgsConstructor;

import static cn.taketoday.persistence.QueryCondition.isEqualsTo;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-30 14:36
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "comments"/*, expire = 30, timeUnit = TimeUnit.SECONDS*/)
public class CommentService {

  private final BlogConfig blogConfig;

  private final UserService userService;

  private final MailService mailService;

  private final CommentConfig commentConfig;

  private final ArticleService articleService;

  private final BloggerService bloggerService;

  private final EntityManager entityManager;

  //  @Cacheable(key = "'ByArticleId_'+#id")
  public List<Comment> getAllByArticleId(long id) {
    List<Comment> commentsToUse = entityManager.find(Comment.class, isEqualsTo("article_id", id));
    if (CollectionUtils.isNotEmpty(commentsToUse)) {
      for (Comment comment : commentsToUse) {
        comment.setUser(userService.getById(comment.getUserId()));
      }
    }
    return commentsToUse;
  }

  public List<CommentItem> fetchByArticleId(long articleId) {
    List<Comment> comments = entityManager.find(Comment.class, new QueryByArticleId(articleId, CommentStatus.CHECKED));

    for (Comment comment : comments) {
      Long userId = comment.getUserId();
      if (userId != null) {
        comment.setUser(userService.getById(userId));
      }
    }

    comments = commentTree(comments);
    List<CommentItem> commentItems = new ArrayList<>();
    for (Comment comment : comments) {
      commentItems.add(CommentItem.forComment(comment));
    }
    return commentItems;
  }

  private List<Comment> commentTree(List<Comment> commentsToUse) {
    if (CollectionUtils.isEmpty(commentsToUse)) {
      return Collections.emptyList();
    }

    List<Comment> comments = new ArrayList<>();
    for (Comment comment : commentsToUse) {
      if (comment.getParentId() == null) {
        comments.add(comment);
      }
    }

    return getComments(comments, commentsToUse);
  }

  public static List<Comment> getComments(List<Comment> commentsRoot, List<Comment> child) {

    for (Comment comment : commentsRoot) {
      comment.setReplies(getReplies(comment.getId(), child));
    }
    return commentsRoot;
  }

  /**
   * 获得回复
   */
  private static List<Comment> getReplies(long parentId, List<Comment> commentsRoot) {

    List<Comment> commentsChild = new ArrayList<>();
    for (Comment comment : commentsRoot) {
      if (Objects.equals(parentId, comment.getParentId())) {
        commentsChild.add(comment);
      }
    }
    for (Comment comment : commentsChild) {
      if (comment.getParentId() != null) {
        comment.setReplies(getReplies(comment.getId(), commentsRoot));
      }
    }
    return commentsChild;
  }

  @Transactional
  public void persist(Comment comment) {
    entityManager.persist(comment);

    //sendMail(comment);
  }

  protected void sendMail(Comment comment) {
    // send admin check mail

    if (comment == null || comment.getStatus() == CommentStatus.CHECKED) {
      return;
    }
    {
      Map<String, Object> dataModel = new HashMap<>();

      dataModel.put("replyUser", userService.getById(comment.getUserId()));
      dataModel.put("article", articleService.getById(comment.getArticleId()));
      dataModel.put("comment", comment);

      mailService.sendTemplateMail(bloggerService.getBlogger().getEmail(),
              blogConfig.name + " 有了新评论请查看",
              dataModel, "/core/mail/admin"
      );
    }
    long commentId = comment.getParentId();
    if (commentId <= 0) {
      return;
    }

    if (!commentConfig.isSendMail()) {
      return;
    }
    Comment parentComment = obtainById(commentId);
    if (Objects.equals(parentComment.getUserId(), comment.getUserId())) {
      return;
    }

    User parentUser = userService.getById(parentComment.getUserId());
    if (parentUser.getNotification()) {

      Map<String, Object> dataModel = new HashMap<>();
      User replyUser = comment.getUser();
      if (replyUser == null) {
        replyUser = userService.getById(comment.getUserId());
      }
      dataModel.put("user", parentUser);
      dataModel.put("replyUser", replyUser);
      dataModel.put("article", articleService.getById(comment.getArticleId()));

      dataModel.put("reply", comment);
      dataModel.put("comment", parentComment);

      mailService.sendTemplateMail(parentUser.getEmail(),
              "您在 " + blogConfig.name + " 的评论有了新回复",
              dataModel, "/core/mail/reply"
      );
    }
  }

  public int count() {
    return entityManager.count(Comment.class).intValue();
  }

  @Transactional
  public void delete(long id) {
    entityManager.delete(Comment.class, id);
  }

  public List<Comment> getLatest() {
    return entityManager.find(Comment.class, Queries.forSelect(select -> select.limit(5)
            .orderBy()
            .desc("id")));
  }

  @Transactional
  public void updateById(Comment comment) {
    entityManager.updateById(comment);
  }

  @Nullable
  public Comment getById(long id) {
    return entityManager.findById(Comment.class, id);
  }

  /**
   * @return {@link Comment} never be null
   */
  public Comment obtainById(long id) {
    Comment comment = getById(id);
    if (comment == null) {
      throw ErrorMessageException.failed("该评论不存在");
    }
    return comment;
  }

  @Transactional
  public void updateStatusById(CommentStatus status, long id) {
    Comment comment1 = new Comment();
    comment1.setId(id);
    comment1.setStatus(status);
    entityManager.updateById(comment1);

//    Comment byId = obtainById(id);
//    byId.setUser(userService.getById(byId.getUserId()));
//    sendMail(byId);

//    if (status == CommentStatus.CHECKED) {
//      sendCheckedMail(byId);
//    }
  }

  private void sendCheckedMail(Comment comment) {
    if (!commentConfig.isSendMail()) {
      return;
    }

    User user = comment.getUser();
    Assert.state(user != null, "用户找不到");
    if (user.getNotification()) {

      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("user", user);
      dataModel.put("article", articleService.getById(comment.getArticleId()));
      dataModel.put("comment", comment);

      mailService.sendTemplateMail(user.getEmail(), //
              "您在 " + blogConfig.name + " 的评论审核通过", //
              dataModel, "/core/mail/checked.ftl"//
      );

    }
  }

  @Transactional
  public void closeEmailNotification() {
  }

  public Pagination<Comment> pagination(CommentConditionForm form, Pageable pageable) {
    return Pagination.from(entityManager.page(Comment.class, form, pageable));
  }

  public Page<Comment> getByUser(User userInfo, Pageable pageable) {
    return entityManager.page(Comment.class, new PageByUserQuery(userInfo), pageable)
            .peek(comment -> comment.setUser(userService.getById(comment.getUserId())));
  }

  @OrderBy(clause = "id DESC")
  static class PageByUserQuery {

    final Long userId;

    PageByUserQuery(User userInfo) {
      this.userId = userInfo.getId();
    }

    public Long getUserId() {
      return userId;
    }

  }

  @OrderBy(clause = "create_at DESC")
  static class QueryByArticleId {

    public final Long articleId;

    public final CommentStatus status;

    QueryByArticleId(Long articleId, CommentStatus status) {
      this.articleId = articleId;
      this.status = status;
    }
  }

}
