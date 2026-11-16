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

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import cn.taketoday.blog.model.Mail;
import cn.taketoday.blog.model.enums.MailStatus;
import cn.taketoday.blog.model.form.MailConditionForm;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.error.ProblemException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.annotation.Value;
import infra.mail.javamail.JavaMailSender;
import infra.mail.javamail.MimeMessageHelper;
import infra.persistence.EntityManager;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.annotation.EntityRef;
import infra.persistence.annotation.OrderByClause;
import infra.persistence.annotation.Where;
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

  private static final int BATCH_SIZE = 20;

  /**
   * 超过该时长仍停留在 SENDING，视为进程中断留下的记录。
   */
  private static final Duration SENDING_TIMEOUT = Duration.ofMinutes(10);

  private final EntityManager entityManager;

  private final BeanFactory beanFactory;

  @Value("${mail.from:${mail.username:}}")
  private String from;

  @Scheduled(initialDelay = 10000, fixedDelay = 5000)
  public void processPending() {
    failInterrupted();
    for (Mail mail : entityManager.slice(Mail.class, new PendingMails(),
            infra.persistence.Pageable.of(1, BATCH_SIZE)).rows()) {
      sendPending(mail.getId());
    }
  }

  void sendPending(long id) {
    // 先确认仍待发送再更新状态，避免重复调度把已完成的记录重新发出。
    Mail mail = entityManager.findById(Mail.class, id);
    if (mail == null || mail.getStatus() != MailStatus.PENDING) {
      return;
    }
    claim(mail);
    try {
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
      mail.setStatus(MailStatus.FAILED);
      mail.setLastError(StringUtils.truncate(
              ex.getClass().getSimpleName() + ": " + ex.getMessage(), 4000));
      update(mail);
      log.error("邮件发送失败，记录 ID: [{}]", id, ex);
      return;
    }
    // SMTP 成功后的状态更新失败不应误标为发送失败。
    mail.setStatus(MailStatus.SENT);
    mail.setSentAt(Instant.now());
    mail.setLastError(null);
    update(mail);
  }

  public Pagination<Mail> pagination(MailConditionForm form, Pageable pageable) {
    return Pagination.from(entityManager.page(Mail.class, form, pageable));
  }

  public void retry(long id) {
    Mail mail = entityManager.findById(Mail.class, id);
    if (mail == null || mail.getStatus() != MailStatus.FAILED) {
      throw new ProblemException(ContentProblems.MAIL_NOT_RETRYABLE);
    }
    mail.setStatus(MailStatus.PENDING);
    update(mail);
  }

  /**
   * 标记为发送中并累加尝试次数，发送结果由后续状态回写覆盖。
   */
  private void claim(Mail mail) {
    mail.setStatus(MailStatus.SENDING);
    mail.setAttempts(mail.getAttempts() == null ? 1 : mail.getAttempts() + 1);
    mail.setLastAttemptAt(Instant.now());
    mail.setLastError(null);
    update(mail);
  }

  /**
   * 中断发送的记录不自动重发，以免重复投递，交由管理员检查。
   */
  private void failInterrupted() {
    Instant cutoff = Instant.now().minus(SENDING_TIMEOUT);
    for (Mail mail : entityManager.find(Mail.class, new InterruptedMails(cutoff))) {
      mail.setStatus(MailStatus.FAILED);
      mail.setLastError("发送中断或超时，请确认后重发");
      update(mail);
    }
  }

  /**
   * 整行覆盖，清空 lastError、sentAt 等上一次发送留下的字段。
   */
  private void update(Mail mail) {
    entityManager.updateById(mail, PropertyUpdateStrategy.always());
  }

  /**
   * 待发送的邮件，按入队顺序领取。
   */
  @OrderByClause("id ASC")
  @EntityRef(Mail.class)
  static class PendingMails {

    public final MailStatus status = MailStatus.PENDING;
  }

  /**
   * 停留在发送中且超过超时时间的邮件。
   */
  @EntityRef(Mail.class)
  static class InterruptedMails {

    public final MailStatus status = MailStatus.SENDING;

    @Where(operator = "<")
    public final Instant lastAttemptAt;

    InterruptedMails(Instant lastAttemptAt) {
      this.lastAttemptAt = lastAttemptAt;
    }
  }

}
