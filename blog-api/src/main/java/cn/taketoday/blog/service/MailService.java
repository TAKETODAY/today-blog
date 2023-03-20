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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Executor;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.blog.model.Operation;
import cn.taketoday.blog.util.MailSender;
import cn.taketoday.blog.util.SendMailException;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Service;
import cn.taketoday.web.InternalServerException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.CustomLog;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 13:33
 */
@Service
@CustomLog
public class MailService {

  private final Executor executor;
  private final BeanFactory beanFactory;
  private final LoggingService loggerService;

  final Configuration configuration;

  public MailService(
          Executor executor,
          BeanFactory beanFactory,
          LoggingService loggerService,
          @Nullable Configuration configuration) {
    this.executor = executor;
    this.beanFactory = beanFactory;
    this.loggerService = loggerService;
    this.configuration = configuration == null ? new Configuration(Configuration.VERSION_2_3_31) : configuration;
  }

  Template getTemplate(String name) throws IOException {
    return configuration.getTemplate(name);
  }

  /**
   * 发送邮件
   *
   * @param to 接收者
   * @param subject 主题
   * @param content 内容
   */
  public void sendMail(String to, String subject, String content) {
    executor.execute(new MailSenderRunnable(to, subject, content, null));
  }

  /**
   * 发送模板邮件
   *
   * @param to 接收者
   * @param subject 主题
   * @param dataModel 内容
   * @param templateName 模板路径
   */
  public void sendTemplateMail(String to, String subject, Map<String, Object> dataModel, String templateName) {
    try (StringWriter result = new StringWriter()) {
      Template template = getTemplate(templateName);
      template.process(dataModel, result);
      executor.execute(new MailSenderRunnable(to, subject, result.toString(), null));
    }
    catch (Exception e) {
      log.error("When sending a mail to:[{}] for subject:[{}]", to, subject, e);
      throw InternalServerException.failed("邮件发送失败", e);
    }
  }

  /**
   * 发送带有附件的邮件
   *
   * @param to 接收者
   * @param subject 主题
   * @param dataModel 内容
   * @param templateName 模板路径
   * @param attachSrc 附件路径
   */

  public void sendAttachMail(String to, String subject, Map<String, Object> dataModel, String templateName, String attachSrc) {
    try (StringWriter result = new StringWriter()) {
      Template template = getTemplate(templateName);
      template.process(dataModel, result);
      executor.execute(new MailSenderRunnable(to, subject, result.toString(), attachSrc));
    }
    catch (Exception e) {
      log.error("When sending a mail to:[{}] for subject:[{}]", to, subject, e);
      throw InternalServerException.failed("邮件发送失败", e);
    }
  }

  MailSender getEmailSender() {
    return beanFactory.getBean(MailSender.class);
  }

  class MailSenderRunnable implements Runnable {

    final String to;
    final String subject;
    final String content;
    final String file;

    public MailSenderRunnable(String to, String subject, String content, String file) {
      this.to = to;
      this.file = file;
      this.content = content;
      this.subject = subject;
    }

    @Override
    public void run() {
      try {
        log.info("send mail: [{}]", to);
        MailSender mailSender = getEmailSender().subject(subject).to(to).html(content);

        if (StringUtils.isNotEmpty(file)) {
          mailSender.attach(new File(file));
        }

        mailSender.send();
      }
      catch (SendMailException e) {
        log.error("can't send mail to: [{}]", to);

        loggerService.afterThrowing(e, new Operation()
                .setUser("邮件系统")
                .setTitle("系统邮件发送出错")
        );
      }
    }
  }

}
