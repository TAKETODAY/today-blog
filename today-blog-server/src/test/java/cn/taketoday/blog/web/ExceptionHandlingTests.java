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

package cn.taketoday.blog.web;

import org.junit.jupiter.api.Test;

import cn.taketoday.blog.problem.CommonProblems;
import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.problem.ProblemResponses;
import cn.taketoday.blog.service.ArticleProblems;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.ProblemDetail;
import infra.web.mock.MockServerHttpExchange;
import infra.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一异常响应测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
class ExceptionHandlingTests {

  private final ExceptionHandling handler = new ExceptionHandling(new ProblemResponses(new PathMatchingPatternResourceLoader()));

  @Test
  void typedBusinessErrorsUseRequestLanguage() {
    MockServerHttpExchange request = new MockServerHttpExchange();
    request.getRequest().addHeader("Accept-Language", "en");
    var response = handler.problem(new ProblemException(ArticleProblems.PASSWORD_INVALID), request);
    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(response.getBody().getDetail()).isEqualTo("Please check the article password and try again.");
  }

  @Test
  void typedBusinessErrorsPreserveCustomHeaders() {
    ProblemException exception = CommonProblems.RATE_LIMIT_EXCEEDED.toException()
            .withHeader("Retry-After", "60", "120");

    assertThat(exception.getBody()).isSameAs(exception.getBody());
    assertThat(exception.getBody().getStatus()).isEqualTo(429);
    assertThat(exception.getHeaders().get("Retry-After")).containsExactly("60", "120");

    var response = handler.problem(exception, new MockServerHttpExchange());

    assertThat(response.getHeaders().get("Retry-After")).containsExactly("60", "120");
  }

  @Test
  void typedBusinessErrorsCanCarryCustomDetail() {
    MockServerHttpExchange request = new MockServerHttpExchange();
    request.getRequest().addHeader("Accept-Language", "en");
    var response = handler.problem(ProblemException.of(ArticleProblems.PASSWORD_INVALID,
            "The password was rejected by the article owner."), request);

    assertThat(response.getBody().getDetail()).isEqualTo("The password was rejected by the article owner.");
    assertThat(response.getBody().getProperties()).containsEntry("code", "article.password_invalid");
  }

  @Test
  void localizesTypedCustomDetailUsingRequestLanguage() {
    MockServerHttpExchange request = new MockServerHttpExchange();
    request.getRequest().addHeader("Accept-Language", "en");
    var response = handler.problem(ProblemException.of(ArticleProblems.PASSWORD_INVALID,
            "request.rate_limit_exceeded"), request);

    assertThat(response.getBody().getDetail()).isEqualTo("Too many requests. Please try again later.");
  }

  @Test
  void customDetailAndTemplateArgumentsAreIndependent() {
    MockServerHttpExchange request = new MockServerHttpExchange();
    var exception = ProblemException.of(cn.taketoday.blog.problem.CommonProblems.PARAMETER_MISSING,
            "A custom parameter message.", "email");

    var response = handler.problem(exception, request);

    assertThat(response.getBody().getDetail()).isEqualTo("A custom parameter message.");
  }

  @Test
  void usesTypedCustomDetailForServerProblems() {
    var response = handler.problem(ProblemException.of(cn.taketoday.blog.problem.CommonProblems.INTERNAL_ERROR,
            "secret SQL"), new MockServerHttpExchange());

    assertThat(response.getBody().getDetail()).isEqualTo("secret SQL");
  }

  @Test
  void replacesFrameworkDetailsAndPreservesHeadersAndStatus() {
    MockServerHttpExchange request = new MockServerHttpExchange();
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Allow", "GET");
    var response = handler.createResponseEntity(ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED),
            headers, HttpStatus.METHOD_NOT_ALLOWED, request);
    assertThat(response.getHeaders().getFirst("Allow")).isEqualTo("GET");
    assertThat(response.getHeaders().getFirst("Content-Type")).contains("application/problem+json");
    assertThat(((ProblemDetail) response.getBody()).getProperties())
            .containsEntry("code", "request.method_not_allowed");
  }

  @Test
  void statusExceptionDoesNotExposeReason() {
    var response = handler.statusException(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "secret configuration"), new MockServerHttpExchange());
    assertThat(((ProblemDetail) response.getBody()).getDetail()).doesNotContain("secret");
  }
}
