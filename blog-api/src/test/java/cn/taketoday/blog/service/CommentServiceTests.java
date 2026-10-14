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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.CommentConfig;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.enums.CommentStatus;
import freemarker.template.Configuration;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.ApplicationEventPublisher;
import infra.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CommentServiceTests {

  final BlogConfig blogConfig = new BlogConfig();

  final CommentConfig commentConfig = new CommentConfig();

  final MailService mailService = mock(MailService.class);

  final BloggerService bloggerService = mock(BloggerService.class);

  final EntityManager entityManager = mock(EntityManager.class);

  final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  final UserService userService = mock(UserService.class);

  final CommentService service = new CommentService(userService, context, entityManager);

  CommentServiceTests() {
    blogConfig.name = "测试博客";
    blogConfig.host = "https://example.com";
    Blogger blogger = new Blogger();
    blogger.setEmail("author@example.com");
    when(bloggerService.getBlogger()).thenReturn(blogger);
    context.registerBean(CommentMailListener.class, () -> new CommentMailListener(blogConfig,
            commentConfig, userService, mailService, bloggerService, entityManager));
    context.refresh();
  }

  @AfterEach
  void cleanup() {
    context.close();
  }

  @Test
  void pendingGuestCommentQueuesAuthorMailFromServiceEvent() {
    Comment comment = comment(1L, "guest@example.com", CommentStatus.CHECKING);
    comment.setParentId(2L);
    service.persist(comment);
    verify(mailService).sendTemplateMail(eq("author@example.com"), eq("测试博客 有了新评论请审核"),
            anyMap(), eq("mail/admin.ftl"));
    verifyNoMoreInteractions(mailService);
  }

  @Test
  void persistingWithoutNotificationListenerOnlySavesComment() {
    CommentService isolated = new CommentService(userService, mock(ApplicationEventPublisher.class), entityManager);
    Comment comment = comment(1L, "guest@example.com", CommentStatus.CHECKING);
    isolated.persist(comment);
    verify(entityManager).persist(comment);
    verifyNoInteractions(mailService);
  }

  @Test
  void approvalNotifiesGuestAndParentAndRepeatedApprovalDoesNotResend() {
    Comment reply = comment(1L, "guest@example.com", CommentStatus.CHECKING);
    reply.setParentId(2L);
    when(entityManager.findById(Comment.class, 1L)).thenReturn(reply);
    when(entityManager.findById(Comment.class, 2L)).thenReturn(comment(2L, "parent@example.com", CommentStatus.CHECKED));
    service.updateStatusById(CommentStatus.CHECKED, 1L);
    verify(mailService).sendTemplateMail(eq("guest@example.com"), eq("您在 测试博客 的评论审核通过"),
            anyMap(), eq("mail/checked.ftl"));
    verify(mailService).sendTemplateMail(eq("parent@example.com"), eq("您在 测试博客 的评论有了新回复"),
            anyMap(), eq("mail/reply.ftl"));
    service.updateStatusById(CommentStatus.CHECKED, 1L);
    verifyNoMoreInteractions(mailService);
  }

  @Test
  void alreadyApprovedReplyNotifiesParentWithoutModerationMail() {
    Comment reply = comment(1L, "author@example.com", CommentStatus.CHECKED);
    reply.setParentId(2L);
    when(entityManager.findById(Comment.class, 2L)).thenReturn(comment(2L, "parent@example.com", CommentStatus.CHECKED));
    service.persist(reply);
    verify(mailService).sendTemplateMail(eq("parent@example.com"), eq("您在 测试博客 的评论有了新回复"),
            anyMap(), eq("mail/reply.ftl"));
    verifyNoMoreInteractions(mailService);
  }

  @Test
  void selfReplyAndDisabledMailDoNotSendNotifications() {
    Comment reply = comment(1L, "guest@example.com", CommentStatus.CHECKED);
    reply.setParentId(2L);
    when(entityManager.findById(Comment.class, 2L)).thenReturn(comment(2L, "guest@example.com", CommentStatus.CHECKED));
    service.persist(reply);
    commentConfig.setSendMail(false);
    service.persist(comment(3L, "guest@example.com", CommentStatus.CHECKING));
    verifyNoInteractions(mailService);
  }

  @Test
  void templatesRenderGuestCommentsAndEscapeHtml() throws Exception {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
    configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
    configuration.setDefaultEncoding("UTF-8");
    Comment comment = comment(1L, "guest@example.com", CommentStatus.CHECKED);
    comment.setContent("<script>alert('x')</script>");
    Map<String, Object> model = Map.of("site", Map.of("name", blogConfig.name, "host", blogConfig.host),
            "comment", comment, "parentComment", comment);
    for (String template : new String[] { "admin", "checked", "reply" }) {
      StringWriter output = new StringWriter();
      configuration.getTemplate("mail/" + template + ".ftl").process(model, output);
      assertThat(output.toString()).contains("&lt;script&gt;").doesNotContain("<script>");
      if (!template.equals("admin")) {
        assertThat(output.toString()).contains("https://example.com/articles/10");
      }
    }
  }


  private Comment comment(long id, String email, CommentStatus status) {
    Comment comment = new Comment();
    comment.setId(id);
    comment.setArticleId(10L);
    comment.setArticleTitle("测试文章");
    comment.setCommenter("访客");
    comment.setContent("评论内容");
    comment.setEmail(email);
    comment.setStatus(status);
    return comment;
  }
}
