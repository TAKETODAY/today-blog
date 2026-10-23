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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import cn.taketoday.blog.model.Mail;
import freemarker.template.Configuration;
import freemarker.template.Template;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;
import infra.web.server.InternalServerException;

/**
 * 将邮件内容保存到数据库，由邮件队列任务发送。
 */
@Service
public class MailService {

  private final EntityManager entityManager;

  final Configuration configuration;

  public MailService(EntityManager entityManager, @Nullable Configuration configuration) {
    this.entityManager = entityManager;
    this.configuration = configuration == null ? new Configuration(Configuration.VERSION_2_3_31) : configuration;
  }

  Template getTemplate(String name) throws IOException {
    return configuration.getTemplate(name);
  }

  @Transactional
  public void sendMail(String to, String subject, String content) {
    enqueue(to, subject, content, null);
  }

  @Transactional
  public void sendTemplateMail(String to, String subject, Map<String, Object> dataModel, String templateName) {
    enqueue(to, subject, render(dataModel, templateName), null);
  }

  @Transactional
  public void sendAttachMail(String to, String subject, Map<String, Object> dataModel,
          String templateName, String attachSrc) {
    enqueue(to, subject, render(dataModel, templateName), attachSrc);
  }

  private String render(Map<String, Object> dataModel, String templateName) {
    try (StringWriter result = new StringWriter()) {
      getTemplate(templateName).process(dataModel, result);
      return result.toString();
    }
    catch (Exception ex) {
      throw new InternalServerException("邮件模板渲染失败", ex);
    }
  }

  private void enqueue(String to, String subject, String content, @Nullable String attachment) {
    Mail mail = new Mail();
    mail.setTo(to);
    mail.setSubject(subject);
    mail.setContent(content);
    mail.setAttachment(attachment);
    mail.setStatus("PENDING");
    mail.setAttempts(0);
    entityManager.persist(mail);
  }
}
