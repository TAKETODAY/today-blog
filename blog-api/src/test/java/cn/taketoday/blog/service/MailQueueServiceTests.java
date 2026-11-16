package cn.taketoday.blog.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import cn.taketoday.blog.model.Mail;
import cn.taketoday.blog.error.ProblemException;
import cn.taketoday.blog.model.enums.MailStatus;
import infra.beans.factory.BeanFactory;
import infra.mail.javamail.JavaMailSender;
import infra.mail.javamail.MimeMessagePreparator;
import infra.persistence.EntityManager;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.Slice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MailQueueServiceTests {

  final EntityManager entityManager = mock(EntityManager.class);
  final BeanFactory beanFactory = mock(BeanFactory.class);
  final JavaMailSender sender = mock(JavaMailSender.class);
  final MailQueueService queue = new MailQueueService(entityManager, beanFactory);

  @Test
  void enqueueStoresRenderedContentWithoutContactingSmtp() {
    MailService service = new MailService(entityManager, null);
    service.sendMail("guest@example.com", "审核通过", "<p>评论已通过</p>");
    ArgumentCaptor<Mail> captor = ArgumentCaptor.forClass(Mail.class);
    verify(entityManager).persist(captor.capture());
    Mail mail = captor.getValue();
    assertThat(mail.getStatus()).isEqualTo(MailStatus.PENDING);
    assertThat(mail.getAttempts()).isZero();
    assertThat(mail.getContent()).isEqualTo("<p>评论已通过</p>");
    assertThat(mail.getSentAt()).isNull();
    verifyNoInteractions(beanFactory);
  }

  @Test
  void statusSerializesAsEnumNameForConsoleContract() {
    JsonMapper mapper = JsonMapper.builder().build();
    assertThat(mapper.writeValueAsString(pendingMail(1L))).contains("\"status\":\"PENDING\"");
  }

  @Test
  void failedSmtpAttemptIsRecordedAndCanBeRequeued() {
    Mail mail = pendingMail(1L);
    when(beanFactory.getBean(JavaMailSender.class)).thenReturn(sender);
    when(entityManager.findById(Mail.class, 1L)).thenReturn(mail);
    doThrow(new IllegalStateException("SMTP unavailable")).when(sender).send(any(MimeMessagePreparator.class));

    queue.sendPending(1L);

    ArgumentCaptor<Mail> failed = ArgumentCaptor.forClass(Mail.class);
    verify(entityManager, times(2)).updateById(failed.capture(), any(PropertyUpdateStrategy.class));
    assertThat(failed.getAllValues().getLast().getStatus()).isEqualTo(MailStatus.FAILED);
    assertThat(failed.getAllValues().getLast().getLastError()).contains("SMTP unavailable");
    assertThat(failed.getAllValues().getLast().getAttempts()).isEqualTo(1);

    queue.retry(1L);

    ArgumentCaptor<Mail> requeued = ArgumentCaptor.forClass(Mail.class);
    verify(entityManager, times(3)).updateById(requeued.capture(), any(PropertyUpdateStrategy.class));
    assertThat(requeued.getAllValues().getLast().getStatus()).isEqualTo(MailStatus.PENDING);
  }

  @Test
  void successfulSendIsMarkedSent() {
    Mail mail = pendingMail(1L);
    when(beanFactory.getBean(JavaMailSender.class)).thenReturn(sender);
    when(entityManager.findById(Mail.class, 1L)).thenReturn(mail);

    queue.sendPending(1L);

    verify(sender).send(any(MimeMessagePreparator.class));
    ArgumentCaptor<Mail> captor = ArgumentCaptor.forClass(Mail.class);
    verify(entityManager, times(2)).updateById(captor.capture(), any(PropertyUpdateStrategy.class));
    assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo(MailStatus.SENT);
    assertThat(captor.getAllValues().getLast().getSentAt()).isNotNull();
    assertThat(captor.getAllValues().getLast().getLastError()).isNull();
    assertThat(captor.getAllValues().getLast().getAttempts()).isEqualTo(1);
  }

  @Test
  void unclaimedMailIsNotSentAndNonFailedMailCannotBeRetried() {
    queue.sendPending(1L);
    verifyNoInteractions(sender, beanFactory);
    verify(entityManager).findById(Mail.class, 1L);

    Mail sent = pendingMail(2L);
    sent.setStatus(MailStatus.SENT);
    when(entityManager.findById(Mail.class, 2L)).thenReturn(sent);
    assertThatThrownBy(() -> queue.retry(2L)).isInstanceOfSatisfying(ProblemException.class, error -> {
      assertThat(error.getProblem()).isEqualTo(ContentProblems.MAIL_NOT_RETRYABLE);
      assertThat(error.getStatusCode().value()).isEqualTo(409);
    });
    verify(entityManager, never()).updateById(any(Mail.class), any(PropertyUpdateStrategy.class));
  }

  @Test
  void pendingScanClaimsAndSendsOneBatch() {
    Mail first = pendingMail(1L);
    Mail second = pendingMail(2L);
    when(entityManager.slice(eq(Mail.class), any(MailQueueService.PendingMails.class), any()))
            .thenReturn(new Slice<>(List.of(first, second), 1, 20, false));
    when(entityManager.findById(Mail.class, 1L)).thenReturn(first);
    when(entityManager.findById(Mail.class, 2L)).thenReturn(second);
    when(beanFactory.getBean(JavaMailSender.class)).thenReturn(sender);

    queue.processPending();

    verify(sender, times(2)).send(any(MimeMessagePreparator.class));
    assertThat(first.getAttempts()).isEqualTo(1);
    assertThat(first.getStatus()).isEqualTo(MailStatus.SENT);
    assertThat(first.getLastAttemptAt()).isNotNull();
    assertThat(second.getStatus()).isEqualTo(MailStatus.SENT);
  }

  @Test
  void interruptedSendingIsMarkedFailed() {
    Mail interrupted = pendingMail(3L);
    interrupted.setStatus(MailStatus.SENDING);
    interrupted.setLastAttemptAt(Instant.now().minusSeconds(3600));
    when(entityManager.find(eq(Mail.class), any(MailQueueService.InterruptedMails.class)))
            .thenReturn(List.of(interrupted));
    when(entityManager.slice(eq(Mail.class), any(MailQueueService.PendingMails.class), any()))
            .thenReturn(new Slice<>(List.of(), 1, 20, false));

    queue.processPending();

    ArgumentCaptor<Mail> captor = ArgumentCaptor.forClass(Mail.class);
    verify(entityManager).updateById(captor.capture(), any(PropertyUpdateStrategy.class));
    assertThat(captor.getValue().getStatus()).isEqualTo(MailStatus.FAILED);
    assertThat(captor.getValue().getLastError()).contains("发送中断或超时");
  }

  private static Mail pendingMail(long id) {
    Mail mail = new Mail();
    mail.setId(id);
    mail.setStatus(MailStatus.PENDING);
    mail.setAttempts(0);
    mail.setTo("guest@example.com");
    mail.setSubject("subject");
    mail.setContent("<p>content</p>");
    return mail;
  }

}
