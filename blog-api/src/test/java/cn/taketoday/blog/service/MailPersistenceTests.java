package cn.taketoday.blog.service;

import org.junit.jupiter.api.Test;

import cn.taketoday.blog.model.Mail;
import cn.taketoday.blog.model.enums.MailStatus;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.persistence.EntityManager;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@WebAPITest
class MailPersistenceTests {

  @Autowired
  MailService mailService;

  @Autowired
  EntityManager entityManager;

  @Autowired
  PlatformTransactionManager transactionManager;

  @Autowired
  MailQueueService mailQueueService;

  @Test
  void queuedMailParticipatesInCallerTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    Long[] id = new Long[1];
    transaction.executeWithoutResult(status -> {
      mailService.sendMail("queue-test@example.com", "rollback-test", "<p>test</p>");
      Mail mail = entityManager.find(Mail.class, java.util.Map.of("subject", "rollback-test")).getFirst();
      id[0] = mail.getId();
      assertThat(mail.getStatus()).isEqualTo(MailStatus.PENDING);
      assertThat(mail.getSentAt()).isNull();
      status.setRollbackOnly();
    });
    assertThat(entityManager.findById(Mail.class, id[0])).isNull();
  }

  @Test
  void failedMailCanBeRequeued() {
    Mail mail = new Mail();
    mail.setTo("retry@example.com");
    mail.setSubject("retry-test");
    mail.setContent("<p>retry</p>");
    mail.setStatus(MailStatus.FAILED);
    mail.setAttempts(2);
    mail.setLastError("SMTP timeout");
    entityManager.persist(mail);

    mailQueueService.retry(mail.getId());

    Mail requeued = entityManager.findById(Mail.class, mail.getId());
    assertThat(requeued.getStatus()).isEqualTo(MailStatus.PENDING);
    assertThat(requeued.getAttempts()).isEqualTo(2);
  }

  @Test
  void interruptedSendingIsSelectedByCutoff() {
    Mail stale = new Mail();
    stale.setTo("stale@example.com");
    stale.setSubject("stale-sending");
    stale.setContent("<p>stale</p>");
    stale.setStatus(MailStatus.SENDING);
    stale.setAttempts(1);
    stale.setLastAttemptAt(java.time.Instant.now().minusSeconds(3600));
    entityManager.persist(stale);

    Mail recent = new Mail();
    recent.setTo("recent@example.com");
    recent.setSubject("recent-sending");
    recent.setContent("<p>recent</p>");
    recent.setStatus(MailStatus.SENDING);
    recent.setAttempts(1);
    recent.setLastAttemptAt(java.time.Instant.now());
    entityManager.persist(recent);

    var cutoff = java.time.Instant.now().minus(java.time.Duration.ofMinutes(10));
    var interrupted = entityManager.find(Mail.class, new MailQueueService.InterruptedMails(cutoff));

    assertThat(interrupted).extracting(Mail::getId).contains(stale.getId()).doesNotContain(recent.getId());
  }

}
