package cn.taketoday.blog.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cn.taketoday.blog.model.Mail;
import infra.beans.factory.BeanFactory;
import infra.jdbc.core.JdbcTemplate;
import infra.mail.javamail.JavaMailSender;
import infra.mail.javamail.MimeMessagePreparator;
import infra.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MailQueueServiceTests {

  final EntityManager entityManager = mock(EntityManager.class);
  final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  final BeanFactory beanFactory = mock(BeanFactory.class);
  final JavaMailSender sender = mock(JavaMailSender.class);
  final MailQueueService queue = new MailQueueService(entityManager, jdbc, beanFactory);

  @Test
  void enqueueStoresRenderedContentWithoutContactingSmtp() {
    MailService service = new MailService(entityManager, null);
    service.sendMail("guest@example.com", "审核通过", "<p>评论已通过</p>");
    ArgumentCaptor<Mail> captor = ArgumentCaptor.forClass(Mail.class);
    verify(entityManager).persist(captor.capture());
    Mail mail = captor.getValue();
    assertThat(mail.getStatus()).isEqualTo("PENDING");
    assertThat(mail.getAttempts()).isZero();
    assertThat(mail.getContent()).isEqualTo("<p>评论已通过</p>");
    assertThat(mail.getSentAt()).isNull();
    verifyNoInteractions(beanFactory);
  }

  @Test
  void failedSmtpAttemptIsRecordedAndCanBeRequeued() {
    when(jdbc.update(anyString(), eq(1L))).thenReturn(1);
    when(beanFactory.getBean(JavaMailSender.class)).thenReturn(sender);
    when(entityManager.findById(Mail.class, 1L)).thenReturn(new Mail());
    doThrow(new IllegalStateException("SMTP unavailable")).when(sender).send(any(MimeMessagePreparator.class));
    queue.sendPending(1L);
    verify(jdbc).update(contains("status = 'FAILED'"), contains("SMTP unavailable"), eq(1L));
    queue.retry(1L);
    verify(jdbc).update("UPDATE t_mail SET status = 'PENDING' WHERE id = ? AND status = 'FAILED'", 1L);
  }

  @Test
  void successfulSendIsMarkedSent() {
    when(jdbc.update(anyString(), eq(1L))).thenReturn(1);
    when(beanFactory.getBean(JavaMailSender.class)).thenReturn(sender);
    when(entityManager.findById(Mail.class, 1L)).thenReturn(new Mail());
    queue.sendPending(1L);
    verify(sender).send(any(MimeMessagePreparator.class));
    verify(jdbc).update(contains("status = 'SENT'"), eq(1L));
  }

  @Test
  void unclaimedMailIsNotSentAndNonFailedMailCannotBeRetried() {
    queue.sendPending(1L);
    verifyNoInteractions(sender, beanFactory, entityManager);
    assertThatThrownBy(() -> queue.retry(1L)).hasMessageContaining("只有发送失败");
  }
}
