/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
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

  private final RepositoryManager repository;
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

  //  @Cacheable(key = "'i'+#id+'p'+#pageNow+'s'+#pageSize")
  public List<Comment> listByArticleId(long id, Pageable pageable) {
    List<Comment> commentsToUse = getAllByArticleId(id);

    if (CollectionUtils.isEmpty(commentsToUse)) {
      return Collections.emptyList();
    }

    List<Comment> comments = new ArrayList<>();
    for (Comment comment : commentsToUse) {
      if (comment.getCommentId() == null) {
        comments.add(comment);
      }
    }

    int size = comments.size();
    int offset = pageable.offset();
    int toIndex = offset + pageable.pageSize();
    if (size <= toIndex) {
      toIndex = size;
    }

    if (offset >= size) {
      return Collections.emptyList();
    }
    List<Comment> subList = comments.subList(offset, toIndex);
    return getComments(new ArrayList<>(subList), commentsToUse);
  }

  public Pagination<Comment> getByArticleId(long articleId, Pageable pageable) {
    try (JdbcConnection connection = repository.open()) {
      try (Query countQuery = connection.createQuery(
              "SELECT COUNT(id) FROM t_comment WHERE `status` = ? and article_id=?")) {
        countQuery.addParameter(CommentStatus.CHECKED);
        countQuery.addParameter(articleId);

        int totalRecord = countQuery.fetchScalar(int.class);
        if (totalRecord < 1) {
          return Pagination.empty();
        }

//        String sql = """
//                SELECT * FROM t_comment WHERE `status` = :status and article_id=:articleId
//                order by create_at DESC LIMIT :offset, :size
//                """;
//        int commentPageSize = commentConfig.getListSize();
//        try (NamedQuery query = repository.createNamedQuery(sql)) {
//          query.addParameter("offset", pageable.offset(commentPageSize));
//          query.addParameter("size", pageable.size(commentPageSize));
//
//          query.addParameter("articleId", articleId);
//          query.addParameter("status", CommentStatus.CHECKED);
//
//          return Pagination.ok(query.fetch(Comment.class), totalRecord, pageable);
//        }

        List<Comment> comments = listByArticleId(articleId, pageable);
        return Pagination.ok(comments, totalRecord, pageable);
      }
    }
  }

  public static List<Comment> getComments(List<Comment> commentsRoot, List<Comment> child) {

    for (Comment comment : commentsRoot) {
      comment.setReplies(getReplies(comment.getId(), child));
    }
    // 集合倒序，最新的评论在最前面
    Collections.reverse(commentsRoot);
    return commentsRoot;
  }

  /**
   * 获得回复
   */
  private static List<Comment> getReplies(long parentId, List<Comment> commentsRoot) {

    List<Comment> commentsChild = new ArrayList<>();
    for (Comment comment : commentsRoot) {
      if (Objects.equals(parentId, comment.getCommentId())) {
        commentsChild.add(comment);
      }
    }
    for (Comment comment : commentsChild) {
      if (comment.getCommentId() != null) {
        comment.setReplies(getReplies(comment.getId(), commentsRoot));
      }
    }
    return commentsChild;
  }

  @Transactional
  public void save(Comment comment) {
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
              blogConfig.getName() + " 有了新评论请查看",
              dataModel, "/core/mail/admin"
      );
    }
    long commentId = comment.getCommentId();
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
              "您在 " + blogConfig.getName() + " 的评论有了新回复",
              dataModel, "/core/mail/reply"
      );
    }
  }

  public int count() {
    try (JdbcConnection connection = repository.open()) {
      try (Query countQuery = connection.createQuery(
              "SELECT COUNT(id) FROM t_comment")) {
        return countQuery.fetchScalar(int.class);
      }
    }
  }

  @Transactional
//  @CacheEvict(allEntries = true)
  public void delete(long id) {
    entityManager.delete(Comment.class, id);
  }

  //  @Cacheable(cacheNames = "LatestComments", unless = "#result.isEmpty()")
  public List<Comment> getLatest() {
    try (JdbcConnection connection = repository.open()) {
      try (Query countQuery = connection.createQuery(
              "SELECT * FROM t_comment ORDER BY id DESC LIMIT 5")) {
        return countQuery.fetch(Comment.class);
      }
    }
  }

  public List<Comment> getByStatus(CommentStatus status, int pageNow, int pageSize) {
    try (Query query = repository.createQuery(""" 
            SELECT * FROM t_comment WHERE `status` = ? ORDER BY id DESC LIMIT ?, ?""")) {
      query.addParameter(status);
      query.addParameter((pageNow - 1) * pageSize);
      query.addParameter(pageSize);
      List<Comment> commentsToUse = query.fetch(Comment.class);
      if (CollectionUtils.isNotEmpty(commentsToUse)) {
        for (Comment comment : commentsToUse) {
          comment.setUser(userService.getById(comment.getUserId()));
        }
      }
      return commentsToUse;
    }
  }

  public int countByStatus(CommentStatus status) {
    try (JdbcConnection connection = repository.open()) {
      try (Query countQuery = connection.createQuery(
              "SELECT COUNT(id) FROM t_comment WHERE `status` = ?")) {
        countQuery.addParameter(status);
        return countQuery.fetchScalar(int.class);
      }
    }
  }

  @Transactional
//  @CacheEvict(allEntries = true)
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
//  @CacheEvict(allEntries = true)
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
              "您在 " + blogConfig.getName() + " 的评论审核通过", //
              dataModel, "/core/mail/checked.ftl"//
      );

    }
  }

  @Transactional
  public void closeEmailNotification() {
  }

  public Pagination<Comment> pagination(Pageable pageable) {
    int count = count();
    List<Comment> comments = getAll(pageable);
    return Pagination.ok(comments, count, pageable);
  }

  private List<Comment> getAll(Pageable pageable) {
    try (Query query = repository.createQuery(""" 
            SELECT * FROM t_comment ORDER BY id DESC LIMIT ?, ?""")) {
      query.addParameter(pageable.offset());
      query.addParameter(pageable.pageSize());
      List<Comment> commentsToUse = query.fetch(Comment.class);
      if (CollectionUtils.isNotEmpty(commentsToUse)) {
        for (Comment comment : commentsToUse) {
          comment.setUser(userService.getById(comment.getUserId()));
        }
      }
      return commentsToUse;
    }
  }

  public List<Comment> getByStatus(CommentStatus valueOf, Pageable pageable) {
    return getByStatus(valueOf, pageable.pageNumber(), pageable.pageSize());
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
}
