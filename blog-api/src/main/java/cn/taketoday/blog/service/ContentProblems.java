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

import cn.taketoday.blog.error.Problem;
import infra.http.HttpStatus;

/**
 * 分类、标签、评论、附件和通知业务的问题定义。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public enum ContentProblems implements Problem {

  CATEGORY_REQUIRED("category.name_required", HttpStatus.BAD_REQUEST),
  CATEGORY_EXISTS("category.already_exists", HttpStatus.CONFLICT),
  CATEGORY_NOT_FOUND("category.not_found", HttpStatus.NOT_FOUND),
  LABEL_REQUIRED("label.name_required", HttpStatus.BAD_REQUEST),
  LABEL_EXISTS("label.already_exists", HttpStatus.CONFLICT),
  LABEL_NOT_FOUND("label.not_found", HttpStatus.NOT_FOUND),
  COMMENT_NOT_FOUND("comment.not_found", HttpStatus.NOT_FOUND),
  COMMENT_TOO_LONG("comment.too_long", HttpStatus.BAD_REQUEST),
  ATTACHMENT_NOT_FOUND("attachment.not_found", HttpStatus.NOT_FOUND),
  ATTACHMENT_ALREADY_SYNCED("attachment.already_synced", HttpStatus.CONFLICT),
  MAIL_NOT_RETRYABLE("notification.mail.not_retryable", HttpStatus.CONFLICT);

  private final String code;

  private final HttpStatus status;

  ContentProblems(String code, HttpStatus status) {
    this.code = code;
    this.status = status;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public HttpStatus status() {
    return status;
  }
}
