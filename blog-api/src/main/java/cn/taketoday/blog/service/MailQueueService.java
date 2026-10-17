package cn.taketoday.blog.service;

import java.io.File;

import cn.taketoday.blog.model.Mail;
import cn.taketoday.blog.model.form.MailConditionForm;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.annotation.Value;
import infra.jdbc.core.JdbcTemplate;
import infra.mail.javamail.JavaMailSender;
import infra.mail.javamail.MimeMessageHelper;
import infra.persistence.EntityManager;
import infra.scheduling.annotation.Scheduled;
import infra.stereotype.Service;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * 只读取已提交的邮件记录，发送失败后保留记录供管理员重试。
 */
@Service
@CustomLog
@RequiredArgsConstructor
public class MailQueueService {

  private final EntityManager entityManager;

  private final JdbcTemplate jdbcTemplate;

  private final BeanFactory beanFactory;

  @Value("${mail.from:${mail.username:}}")
  private String from;

  @Scheduled(initialDelay = 10000, fixedDelay = 5000)
  public void processPending() {
    // 进程退出等导致的中断发送不自动重发，以免重复投递，交由管理员检查。
    jdbcTemplate.update("""
            UPDATE t_mail SET status = 'FAILED', last_error = '发送中断或超时，请确认后重发'
            WHERE status = 'SENDING' AND last_attempt_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 MINUTE)
            """);
    for (Long id : jdbcTemplate.queryForList(
            "SELECT id FROM t_mail WHERE status = 'PENDING' ORDER BY id LIMIT 20", Long.class)) {
      sendPending(id);
    }
  }

  void sendPending(long id) {
    // 原子领取，防止多个实例或重复调度发送同一条记录。
    int claimed = jdbcTemplate.update("""
            UPDATE t_mail SET status = 'SENDING', attempts = attempts + 1, last_attempt_at = CURRENT_TIMESTAMP,
            last_error = NULL WHERE id = ? AND status = 'PENDING'
            """, id);
    if (claimed != 1) {
      return;
    }
    try {
      Mail mail = entityManager.findById(Mail.class, id);
      if (mail == null) {
        throw new IllegalStateException("邮件记录不存在");
      }
      beanFactory.getBean(JavaMailSender.class).send(message -> {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        if (StringUtils.hasText(from)) {
          helper.setFrom(from);
        }
        helper.setTo(mail.getTo());
        helper.setSubject(mail.getSubject());
        helper.setText(mail.getContent(), true);
        if (StringUtils.hasText(mail.getAttachment())) {
          File file = new File(mail.getAttachment());
          helper.addAttachment(file.getName(), file);
        }
      });
    }
    catch (Exception ex) {
      jdbcTemplate.update("UPDATE t_mail SET status = 'FAILED', last_error = ? WHERE id = ? AND status = 'SENDING'",
              StringUtils.truncate(ex.getClass().getSimpleName() + ": " + ex.getMessage(), 4000), id);
      log.error("邮件发送失败，记录 ID: [{}]", id, ex);
      return;
    }
    // SMTP 成功后的状态更新失败不应误标为发送失败。
    jdbcTemplate.update("""
            UPDATE t_mail SET status = 'SENT', sent_at = CURRENT_TIMESTAMP, last_error = NULL
            WHERE id = ? AND status = 'SENDING'
            """, id);
  }

  public Pagination<Mail> pagination(MailConditionForm form, Pageable pageable) {
    return Pagination.from(entityManager.page(Mail.class, form, pageable));
  }

  public void retry(long id) {
    int updated = jdbcTemplate.update("UPDATE t_mail SET status = 'PENDING' WHERE id = ? AND status = 'FAILED'", id);
    if (updated != 1) {
      throw ErrorMessageException.failed("只有发送失败的邮件可以重发");
    }
  }
}
