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

import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.problem.ProblemResponses;
import cn.taketoday.blog.service.ArticleProblems;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.ProblemDetail;
import infra.web.mock.MockHttpContext;
import infra.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一异常响应测试。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
class ExceptionHandlingTests {

  private final ExceptionHandling handler = new ExceptionHandling(new ProblemResponses());

  @Test
  void typedBusinessErrorsUseRequestLanguage() {
    MockHttpContext request = new MockHttpContext();
    request.getRequest().addHeader("Accept-Language", "en");
    var response = handler.problem(new ProblemException(ArticleProblems.PASSWORD_INVALID), request);
    assertThat(response.getStatusCode().value()).isEqualTo(403);
    assertThat(response.getBody().getDetail()).isEqualTo("Please check the article password and try again.");
  }

  @Test
  void preservesLegacyClientMessageButHidesServerDetails() {
    MockHttpContext request = new MockHttpContext();
    var client = handler.errorMessage(ErrorMessageException.failed("业务说明", HttpStatus.CONFLICT), request);
    assertThat(client.getBody().getDetail()).isEqualTo("业务说明");
    assertThat(client.getBody().getProperties()).containsEntry("code", "resource.conflict");
    var server = handler.errorMessage(ErrorMessageException.failed("secret SQL", HttpStatus.INTERNAL_SERVER_ERROR), request);
    assertThat(server.getBody().getDetail()).doesNotContain("secret SQL");
  }

  @Test
  void replacesFrameworkDetailsAndPreservesHeadersAndStatus() {
    MockHttpContext request = new MockHttpContext();
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
            "secret configuration"), new MockHttpContext());
    assertThat(((ProblemDetail) response.getBody()).getDetail()).doesNotContain("secret");
  }
}
