package cn.taketoday.blog.service;

import org.junit.jupiter.api.Test;

import cn.taketoday.blog.model.Mail;
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

  @Test
  void queuedMailParticipatesInCallerTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    Long[] id = new Long[1];
    transaction.executeWithoutResult(status -> {
      mailService.sendMail("queue-test@example.com", "rollback-test", "<p>test</p>");
      Mail mail = entityManager.find(Mail.class, java.util.Map.of("subject", "rollback-test")).getFirst();
      id[0] = mail.getId();
      assertThat(mail.getStatus()).isEqualTo("PENDING");
      assertThat(mail.getSentAt()).isNull();
      status.setRollbackOnly();
    });
    assertThat(entityManager.findById(Mail.class, id[0])).isNull();
  }
}
