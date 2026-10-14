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

package cn.taketoday.blog.service;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.blog.event.CommentApprovedEvent;
import cn.taketoday.blog.event.CommentCreatedEvent;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.CommentItem;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.model.form.CommentConditionForm;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import infra.beans.aot.RegisterBeanMetadata;
import infra.cache.annotation.CacheConfig;
import infra.context.ApplicationEventPublisher;
import infra.persistence.EntityManager;
import infra.persistence.annotation.OrderByClause;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;
import infra.util.CollectionUtils;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-30 14:36
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "comments"/*, expire = 30, timeUnit = TimeUnit.SECONDS*/)
public class CommentService {

  private final UserService userService;

  private final ApplicationEventPublisher eventPublisher;

  private final EntityManager entityManager;

  //  @Cacheable(key = "'ByArticleId_'+#id")
  public List<Comment> getAllByArticleId(long id) {
    List<Comment> commentsToUse = entityManager.find(Comment.class, Map.of("article_id", id));
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
    eventPublisher.publishEvent(new CommentCreatedEvent(this, comment));
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
    Comment previous = obtainById(comment.getId());
    entityManager.updateById(comment);
    if (comment.getStatus() == CommentStatus.CHECKED && previous.getStatus() != CommentStatus.CHECKED) {
      eventPublisher.publishEvent(new CommentApprovedEvent(this, obtainById(comment.getId())));
    }
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
    Comment previous = obtainById(id);
    if (previous.getStatus() == status) {
      return;
    }
    Comment comment1 = new Comment();
    comment1.setId(id);
    comment1.setStatus(status);
    entityManager.updateById(comment1);

    if (status == CommentStatus.CHECKED) {
      previous.setStatus(status);
      eventPublisher.publishEvent(new CommentApprovedEvent(this, previous));
    }
  }

  public Pagination<Comment> pagination(CommentConditionForm form, Pageable pageable) {
    return Pagination.from(entityManager.page(Comment.class, form, pageable));
  }

  @RegisterBeanMetadata
  @OrderByClause("create_at DESC")
  static class QueryByArticleId {

    public final Long articleId;

    public final CommentStatus status;

    QueryByArticleId(Long articleId, CommentStatus status) {
      this.articleId = articleId;
      this.status = status;
    }
  }

}
