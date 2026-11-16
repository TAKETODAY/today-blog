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

package cn.taketoday.blog.error;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import cn.taketoday.blog.util.ObjectUtils;
import infra.context.support.ResourceBundleMessageSource;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.stereotype.Component;
import infra.web.HttpContext;
import infra.validation.ObjectError;
import jakarta.validation.ConstraintViolation;
import lombok.CustomLog;

/**
 * MVC 和早期过滤器共享的本地化问题响应，不依赖线程语言上下文。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
@CustomLog
@Component
public final class ProblemResponses {

  private static final List<Locale> SUPPORTED = List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);

  private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();

  public ProblemResponses() {
    messages.setBasename("i18n/errors");
    messages.setDefaultCharset(StandardCharsets.UTF_8);
    messages.setFallbackToSystemLocale(false);
  }

  public Locale locale(HttpContext request) {
    String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    if (header != null && !header.isBlank()) {
      try {
        Locale match = Locale.lookup(Locale.LanguageRange.parse(header), SUPPORTED);
        if (match != null) {
          return match;
        }
      }
      catch (IllegalArgumentException ignored) {
        // 非法语言头不应导致错误处理再次失败。
      }
    }
    return Locale.SIMPLIFIED_CHINESE;
  }

  public ProblemDetail create(Problem type, HttpContext request, Object... arguments) {
    Locale locale = locale(request);
    ProblemDetail problem = ProblemDetail.forStatus(type.status());
    problem.setType(type.type());
    problem.setTitle(message(type.code() + ".title", locale, "请求失败", "Request failed"));
    problem.setDetail(message(type.code() + ".detail", locale,
            "暂时无法完成操作，请稍后重试。", "Unable to complete the operation. Please try again later.", arguments));
    problem.setProperty("code", type.code());
    return problem;
  }

  private String message(String key, Locale locale, String chinese, String english, Object... arguments) {
    String fallback = locale.getLanguage().equals("en") ? english : chinese;
    try {
      String value = messages.getMessage(key, arguments, locale);
      return value;
    }
    catch (RuntimeException ex) {
      log.warn("Cannot resolve error message '{}' for {}", key, locale, ex);
      return fallback;
    }
  }

  public String validationMessage(ObjectError error, HttpContext request) {
    String template = error.getDefaultMessage();
    if (error.contains(ConstraintViolation.class)) {
      template = error.unwrap(ConstraintViolation.class).getMessageTemplate();
    }
    String key = "validation." + error.getCode();
    if (template != null && template.startsWith("{validation.") && template.endsWith("}")) {
      key = template.substring(1, template.length() - 1);
    }
    return message(key, locale(request), "输入内容不合法。", "The input value is invalid.");
  }

  public ResponseEntity<ProblemDetail> response(Problem type, HttpContext request, Object... arguments) {
    return response(create(type, request, arguments), null, type.status(), request);
  }

  public ResponseEntity<ProblemDetail> response(ProblemDetail problem, @Nullable HttpHeaders headers,
          HttpStatusCode status, HttpContext request) {
    problem.setStatus(status.value());
    prepare(request);
    return ResponseEntity.status(status).headers(headers)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
  }

  public void write(ProblemDetail problem, HttpContext request) throws IOException {
    request.setStatus(problem.getStatus());
    prepare(request);
    ObjectUtils.writeValue(request, problem);
  }

  public void prepare(HttpContext request) {
    request.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    request.setHeader(HttpHeaders.CONTENT_LANGUAGE, locale(request).toLanguageTag());
    request.responseHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
  }
}
