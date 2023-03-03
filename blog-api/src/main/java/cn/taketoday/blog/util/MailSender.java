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
package cn.taketoday.blog.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Prototype;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

@Prototype
public class MailSender {

  private static String userName;
  private static String nickName;
  private static String password;

  private static Session session;

  private String text;
  private String html;
  private MimeMessage msg;

  private final List<MimeBodyPart> attachments = new ArrayList<>();

  static {
    final Properties props = System.getProperties(); // "mail."

    nickName = props.getProperty("mail.nickName");
    userName = props.getProperty("mail.userName");
    password = props.getProperty("mail.password");

    session = Session.getInstance(props, new Authenticator() {

      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
      }
    });
  }

  /**
   * set email subject
   *
   * @param subject subject title
   */
  public MailSender subject(String subject) throws SendMailException {
    try {
      msg = new MimeMessage(session);
      msg.setSubject(subject, BlogConstant.DEFAULT_ENCODING);
      msg.setFrom(new InternetAddress(MimeUtility.encodeText(nickName) + " <" + userName + ">"));
    }
    catch (Exception e) {
      throw new SendMailException(e);
    }
    return this;
  }

  public MailSender replyTo(String... replyTo) throws SendMailException {
    String result = Arrays.asList(replyTo).toString().replaceAll("(^\\[|\\]$)", "").replace(", ", ",");
    try {
      msg.setReplyTo(InternetAddress.parse(result));
    }
    catch (Exception e) {
      throw new SendMailException(e);
    }
    return this;
  }

  public MailSender replyTo(String replyTo) throws SendMailException {
    try {
      msg.setReplyTo(InternetAddress.parse(replyTo.replace(";", ",")));
    }
    catch (Exception e) {
      throw new SendMailException(e);
    }
    return this;
  }

  public MailSender to(String... to) throws SendMailException {
    try {
      return addRecipients(to, Message.RecipientType.TO);
    }
    catch (MessagingException e) {
      throw new SendMailException(e);
    }
  }

  public MailSender to(String to) throws SendMailException {
    try {
      return addRecipient(to, Message.RecipientType.TO);
    }
    catch (MessagingException e) {
      throw new SendMailException(e);
    }
  }

  public MailSender cc(String... cc) throws SendMailException {
    try {
      return addRecipients(cc, Message.RecipientType.CC);
    }
    catch (MessagingException e) {
      throw new SendMailException(e);
    }
  }

  public MailSender cc(String cc) throws SendMailException {
    try {
      return addRecipient(cc, Message.RecipientType.CC);
    }
    catch (MessagingException e) {
      throw new SendMailException(e);
    }
  }

  public MailSender bcc(String... bcc) throws SendMailException {
    try {
      return addRecipients(bcc, Message.RecipientType.BCC);
    }
    catch (MessagingException e) {
      throw new SendMailException(e);
    }
  }

  public MailSender bcc(String bcc) throws MessagingException {
    return addRecipient(bcc, Message.RecipientType.BCC);
  }

  private MailSender addRecipients(String[] recipients, Message.RecipientType type) throws MessagingException {
    String result = Arrays.asList(recipients).toString().replace("(^\\[|\\]$)", "").replace(", ", ",");
    msg.setRecipients(type, InternetAddress.parse(result));
    return this;
  }

  private MailSender addRecipient(String recipient, Message.RecipientType type) throws MessagingException {
    msg.setRecipients(type, InternetAddress.parse(recipient.replace(";", ",")));
    return this;
  }

  public MailSender text(String text) {
    this.text = text;
    return this;
  }

  public MailSender html(String html) {
    this.html = html;
    return this;
  }

  public MailSender attach(File file) throws SendMailException {
    attachments.add(createAttachment(file, file.getName()));
    return this;
  }

  public MailSender attach(File file, String fileName) throws SendMailException {
    attachments.add(createAttachment(file, fileName));
    return this;
  }

  public MailSender attachURL(URL url, String fileName) throws SendMailException {
    attachments.add(createURLAttachment(url, fileName));
    return this;
  }

  private MimeBodyPart createAttachment(File file, String fileName) throws SendMailException {
    MimeBodyPart attachmentPart = new MimeBodyPart();
    FileDataSource fds = new FileDataSource(file);
    try {
      attachmentPart.setDataHandler(new DataHandler(fds));
      attachmentPart.setFileName(null == fileName ? MimeUtility.encodeText(fds.getName()) : MimeUtility.encodeText(fileName));
    }
    catch (Exception e) {
      throw new SendMailException(e);
    }
    return attachmentPart;
  }

  private MimeBodyPart createURLAttachment(URL url, String fileName) throws SendMailException {
    MimeBodyPart attachmentPart = new MimeBodyPart();

    DataHandler dataHandler = new DataHandler(url);
    try {
      attachmentPart.setDataHandler(dataHandler);
      attachmentPart.setFileName(MimeUtility.encodeText(fileName));
    }
    catch (Exception e) {
      throw new SendMailException(e);
    }
    return attachmentPart;
  }

  public void send() throws SendMailException {
    if (text == null && html == null) {
      throw new IllegalArgumentException("At least one context has to be provided: Text or Html");
    }

    MimeMultipart cover;
    boolean usingAlternative = false;

    try {
      if (text != null && html == null) {
        // TEXT ONLY
        cover = new MimeMultipart("mixed");
        cover.addBodyPart(textPart());
      }
      else if (text == null && StringUtils.isNotEmpty(html)) {
        // HTML ONLY
        cover = new MimeMultipart("mixed");
        cover.addBodyPart(htmlPart());
      }
      else {
        // HTML + TEXT
        cover = new MimeMultipart("alternative");
        cover.addBodyPart(textPart());
        cover.addBodyPart(htmlPart());
        usingAlternative = true;
      }

      MimeMultipart content = cover;
      if (usingAlternative && !attachments.isEmpty()) {
        content = new MimeMultipart("mixed");
        content.addBodyPart(toBodyPart(cover));
      }

      for (MimeBodyPart attachment : attachments) {
        content.addBodyPart(attachment);
      }

      msg.setContent(content);
      msg.setSentDate(new Date());
      Transport.send(msg);
    }
    catch (Exception e) {
      throw new SendMailException(e);
    }
  }

  private MimeBodyPart toBodyPart(MimeMultipart cover) throws MessagingException {
    MimeBodyPart wrap = new MimeBodyPart();
    wrap.setContent(cover);
    return wrap;
  }

  private MimeBodyPart textPart() throws MessagingException {
    MimeBodyPart bodyPart = new MimeBodyPart();
    bodyPart.setText(text);
    return bodyPart;
  }

  private MimeBodyPart htmlPart() throws MessagingException {
    MimeBodyPart bodyPart = new MimeBodyPart();
    bodyPart.setContent(html, MediaType.TEXT_HTML_VALUE);
    return bodyPart;
  }

}
