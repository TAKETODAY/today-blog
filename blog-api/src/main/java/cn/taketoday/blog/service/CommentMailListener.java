package cn.taketoday.blog.service;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.event.CommentApprovedEvent;
import cn.taketoday.blog.event.CommentCreatedEvent;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.util.StringUtils;
import infra.context.event.EventListener;
import infra.persistence.EntityManager;
import infra.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * 根据评论生命周期事件发送邮件通知。
 */
@Component
@RequiredArgsConstructor
public class CommentMailListener {

  private final BlogConfig blogConfig;

  private final CommentConfig commentConfig;

  private final UserService userService;

  private final MailService mailService;

  private final BloggerService bloggerService;

  private final EntityManager entityManager;

  @EventListener
  public void onCommentCreated(CommentCreatedEvent event) {
    Comment comment = event.getComment();
    if (comment.getStatus() == CommentStatus.CHECKING) {
      sendModerationMail(comment);
    }
    else if (comment.getStatus() == CommentStatus.CHECKED) {
      sendReplyMail(comment);
    }
  }

  @EventListener
  public void onCommentApproved(CommentApprovedEvent event) {
    Comment comment = event.getComment();
    sendCheckedMail(comment);
    sendReplyMail(comment);
  }

  private void sendModerationMail(Comment comment) {
    if (!commentConfig.isSendMail()) {
      return;
    }
    String email = bloggerService.getBlogger().getEmail();
    if (StringUtils.hasText(email)) {
      mailService.sendTemplateMail(email, blogConfig.name + " 有了新评论请审核",
              mailModel(comment), "mail/admin.ftl");
    }
  }

  private void sendReplyMail(Comment comment) {
    Long commentId = comment.getParentId();
    if (!commentConfig.isSendMail() || commentId == null || commentId <= 0) {
      return;
    }
    Comment parentComment = entityManager.findById(Comment.class, commentId);
    if (parentComment == null || parentComment.getStatus() != CommentStatus.CHECKED
            || !Objects.equals(parentComment.getArticleId(), comment.getArticleId())) {
      return;
    }
    String email = notificationEmail(parentComment);
    if (!StringUtils.hasText(email)
            || (comment.getUserId() != null && Objects.equals(parentComment.getUserId(), comment.getUserId()))
            || email.equalsIgnoreCase(comment.getEmail())) {
      return;
    }
    Map<String, Object> dataModel = mailModel(comment);
    dataModel.put("parentComment", parentComment);
    mailService.sendTemplateMail(email, "您在 " + blogConfig.name + " 的评论有了新回复",
            dataModel, "mail/reply.ftl");
  }

  private void sendCheckedMail(Comment comment) {
    if (!commentConfig.isSendMail()) {
      return;
    }
    String email = notificationEmail(comment);
    if (StringUtils.hasText(email)) {
      mailService.sendTemplateMail(email, "您在 " + blogConfig.name + " 的评论审核通过",
              mailModel(comment), "mail/checked.ftl");
    }
  }

  private Map<String, Object> mailModel(Comment comment) {
    Map<String, Object> dataModel = new HashMap<>();
    Map<String, Object> site = new HashMap<>();
    site.put("name", blogConfig.name);
    site.put("host", blogConfig.host);
    dataModel.put("site", site);
    dataModel.put("comment", comment);
    return dataModel;
  }

  @Nullable
  private String notificationEmail(Comment comment) {
    User user = comment.getUserId() == null ? null : userService.getById(comment.getUserId());
    if (user != null && !user.getNotification()) {
      return null;
    }
    return StringUtils.hasText(comment.getEmail()) ? comment.getEmail() : user == null ? null : user.getEmail();
  }

}
