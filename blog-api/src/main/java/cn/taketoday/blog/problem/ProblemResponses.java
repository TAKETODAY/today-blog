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

package cn.taketoday.blog.problem;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import infra.context.support.ResourceBundleMessageSource;
import infra.core.io.PatternResourceLoader;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.stereotype.Component;
import infra.validation.ObjectError;
import infra.web.HttpContext;
import infra.web.util.WebUtils;
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

  private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();

  public ProblemResponses(PatternResourceLoader patternResourceLoader) {
    messages.setBasenames(discoverMessageBasenames(patternResourceLoader).toArray(String[]::new));
    messages.setDefaultCharset(StandardCharsets.UTF_8);
    messages.setDefaultLocale(Locale.CHINESE);
  }

  /**
   * 启动时发现类路径目录及 JAR 中的消息资源，按 basename 排序以保持加载顺序稳定。
   */
  static List<String> discoverMessageBasenames(PatternResourceLoader loader) {
    TreeSet<String> basenames = new TreeSet<>();
    try {
      for (var resource : loader.getResources("classpath*:problems/**/messages.properties")) {
        String url = resource.getURL().toExternalForm();
        String basename = messageBasename(loader, url);
        if (!basenames.add(basename)) {
          throw new IllegalStateException("Duplicate problem message basename: " + basename);
        }
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException("Cannot discover problem messages", ex);
    }
    if (basenames.isEmpty()) {
      throw new IllegalStateException("No problem messages found under classpath*:problems/");
    }
    return List.copyOf(basenames);
  }

  /**
   * 通过类路径精确查找确认相对路径，避免物理目录中的同名 problems 路径干扰。
   */
  private static String messageBasename(PatternResourceLoader loader, String url) throws IOException {
    int index = url.indexOf("/problems/");
    while (index >= 0) {
      String path = url.substring(index + 1);
      for (var candidate : loader.getResources("classpath*:" + path)) {
        if (candidate.getURL().toExternalForm().equals(url)) {
          return path.substring(0, path.length() - ".properties".length());
        }
      }
      index = url.indexOf("/problems/", index + 1);
    }
    throw new IllegalStateException("Cannot determine problem message basename: " + url);
  }

  private static Locale getLocale(HttpContext request) {
    return WebUtils.resolveLocale(request);
  }

  public ProblemDetail create(Problem problem, HttpContext request, Object... arguments) {
    return createWithDetail(problem, request, null, arguments);
  }

  /**
   * 创建问题响应；请求级 detail 作为消息键按请求语言解析，找不到消息键时保留原文，并覆盖默认 detail。
   */
  public ProblemDetail createWithDetail(Problem problem, HttpContext request, @Nullable String customDetail, Object... arguments) {
    Locale locale = getLocale(request);
    String code = problem.code();
    ProblemDetail detail = ProblemDetail.forStatus(problem.status());
    detail.setTitle(message(code + ".title", locale, "请求失败", "Request failed"));
    detail.setDetail(customDetail != null
            ? customMessage(customDetail, locale, arguments)
            : message(code, locale, "暂时无法完成操作，请稍后重试。", "Unable to complete the operation. Please try again later.", arguments));
    detail.setProperty("code", code);
    return detail;
  }

  private String message(String key, Locale locale, String chinese, String english, Object... arguments) {
    try {
      return messages.getMessage(key, arguments, locale);
    }
    catch (RuntimeException ex) {
      log.warn("Cannot resolve error message '{}' for {}", key, locale, ex);
      return locale.getLanguage().equals("en") ? english : chinese;
    }
  }

  private String customMessage(String keyOrMessage, Locale locale, Object... arguments) {
    try {
      String message = messages.getMessage(keyOrMessage, arguments, null, locale);
      return message != null ? message : keyOrMessage;
    }
    catch (RuntimeException ex) {
      return keyOrMessage;
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
    return message(key, getLocale(request), "输入内容不合法。", "The input value is invalid.");
  }

  public ResponseEntity<ProblemDetail> response(Problem type, HttpContext request, Object... arguments) {
    return response(create(type, request, arguments), null, type.status(), request);
  }

  public ResponseEntity<ProblemDetail> response(ProblemDetail problem, @Nullable HttpHeaders headers,
          HttpStatusCode status, HttpContext request) {
    problem.setStatus(status.value());
    prepare(request);
    return ResponseEntity.status(status)
            .headers(headers)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
  }

  public void prepare(HttpContext request) {
    request.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    request.setHeader(HttpHeaders.CONTENT_LANGUAGE, getLocale(request).toLanguageTag());
    request.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE);
  }

}
