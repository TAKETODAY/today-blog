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

package cn.taketoday.blog.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.repository.CommentRepository;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.lang.Assert;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.NotFoundException;
import lombok.RequiredArgsConstructor;

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
  private final CommentRepository commentRepository;

  //  @Cacheable(key = "'countById_'+#id")
  public int countByArticleId(long id) {
    return commentRepository.getRecordByArticleId(id);
  }

  //  @Cacheable(key = "'ByArticleId_'+#id")
  public List<Comment> getAllByArticleId(long id) {
    List<Comment> commentsToUse = commentRepository.getArticleComment(id);
    if (CollectionUtils.isNotEmpty(commentsToUse)) {
      for (Comment comment : commentsToUse) {
        comment.setUser(userService.getById(comment.getUserId()));
      }
    }
    return commentsToUse;
  }

  //  @Cacheable(key = "'i'+#id+'p'+#pageNow+'s'+#pageSize")
  public List<Comment> getByArticleId(long id, int pageNow, int pageSize) {
    List<Comment> commentsToUse = getAllByArticleId(id);

    if (CollectionUtils.isEmpty(commentsToUse)) {
      return Collections.emptyList();
    }

    List<Comment> comments = new ArrayList<>();
    for (Comment comment : commentsToUse) {
      if (comment.getCommentId() == 0) {
        comments.add(comment);
      }
    }

    int size = comments.size();
    int offset = (pageNow - 1) * pageSize;
    int toIndex = offset + pageSize;
    if (size <= toIndex) {
      toIndex = size;
    }
    List<Comment> subList = comments.subList(offset, toIndex);
    return getComments(new ArrayList<>(subList), commentsToUse);
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
      if (comment.getCommentId() == parentId) {
        commentsChild.add(comment);
      }
    }
    for (Comment comment : commentsChild) {
      if (comment.getCommentId() != 0) {
        comment.setReplies(getReplies(comment.getId(), commentsRoot));
      }
    }
    return commentsChild;
  }

  @Transactional
  public void save(Comment comment) {
    commentRepository.save(comment);

    sendMail(comment);
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

      mailService.sendTemplateMail(bloggerService.getBlogger().getEmail(), //
              blogConfig.getName() + " 有了新评论请查看", //
              dataModel, "/core/mail/admin"//
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

      mailService.sendTemplateMail(parentUser.getEmail(), //
              "您在 " + blogConfig.getName() + " 的评论有了新回复", //
              dataModel, "/core/mail/reply"//
      );
    }
  }

  public int count() {
    return commentRepository.getTotalRecord();
  }

  @Transactional
//  @CacheEvict(allEntries = true)
  public void delete(long id) {
    commentRepository.deleteById(id);
  }

  //  @Cacheable(cacheNames = "LatestComments", unless = "#result.isEmpty()")
  public List<Comment> getLatest() {
    return commentRepository.findLatest();
  }

  public List<Comment> getByStatus(CommentStatus status, int pageNow, int pageSize) {
    List<Comment> commentsToUse = commentRepository.findByStatus(
            status, (pageNow - 1) * pageSize, pageSize);
    if (CollectionUtils.isNotEmpty(commentsToUse)) {
      for (Comment comment : commentsToUse) {
        comment.setUser(userService.getById(comment.getUserId()));
      }
    }
    return commentsToUse;
  }

  public int countByStatus(CommentStatus status) {
    return commentRepository.getRecord(status);
  }

  @Transactional
//  @CacheEvict(allEntries = true)
  public void update(Comment comment) {
    comment.setLastModify(System.currentTimeMillis());
    commentRepository.update(comment);
  }

  public Comment getById(long id) {
    return commentRepository.findById(id);
  }

  /**
   * @return {@link Comment} never be null
   */

  public Comment obtainById(long id) {
    Comment comment = getById(id);
    if (comment == null) {
      throw new NotFoundException("该评论不存在");
    }
    return comment;
  }

  @Transactional
//  @CacheEvict(allEntries = true)
  public void updateStatusById(CommentStatus status, long id) {

    commentRepository.updateStatus(status, id);
    Comment byId = obtainById(id);
    byId.setUser(userService.getById(byId.getUserId()));
    sendMail(byId);

    if (status == CommentStatus.CHECKED) {
      sendCheckedMail(byId);
    }
  }

  private void sendCheckedMail(Comment comment) {
    if (!commentConfig.isSendMail()) {
      return;
    }
    if (!comment.getSendMail()) {
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
      commentRepository.checked(comment.getId());
    }
  }

  //  @Cacheable(key = "'u_'+#userInfo.id+'p'+#pageNow+'s'+#pageSize")
  public List<Comment> getByUser(User userInfo, int pageNow, int pageSize) {
    return commentRepository.findByUser(userInfo, (pageNow - 1) * pageSize, pageSize);
  }

  //  @Cacheable(key = "'ByUser_'+#userInfo.id")
  public int countByUser(User userInfo) {
    return commentRepository.getRecordByUser(userInfo);
  }

  @Transactional
  public void closeEmailNotification() {
    commentRepository.closeAllNotification();
  }

  public Pagination<Comment> pagination(Pageable pageable) {
    int count = count();
    List<Comment> comments = getAll(pageable);
    return Pagination.ok(comments, count, pageable);
  }

  private List<Comment> getAll(Pageable pageable) {
    int pageSize = pageable.size();
    int pageNow = pageable.current();
    return commentRepository.find((pageNow - 1) * pageSize, pageSize);
  }

  public List<Comment> getByStatus(CommentStatus valueOf, Pageable pageable) {
    return getByStatus(valueOf, pageable.current(), pageable.size());
  }

  public List<Comment> getByUser(User userInfo, Pageable pageable) {
    return getByUser(userInfo, pageable.current(), pageable.size());
  }

}
