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

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import cn.taketoday.blog.problem.CommonProblems;
import cn.taketoday.blog.problem.ProblemResponses;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.web.HttpContext;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.server.error.ErrorController;
import infra.web.server.error.SendErrorHandler;

/**
 * 为错误路由和服务器的 sendError 入口生成统一的问题响应。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/18
 */
@RestController
class ProblemErrorController implements ErrorController, SendErrorHandler {

  private final ProblemResponses problems;

  ProblemErrorController(ProblemResponses problems) {
    this.problems = problems;
  }

  @RequestMapping("${server.error.path:${error.path:/error}}")
  public ResponseEntity<ProblemDetail> error(HttpContext request) {
    HttpStatusCode status = HttpStatusCode.valueOf(request.getStatus());
    if (!status.isError()) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return problems.response(problems.create(CommonProblems.forStatus(status), request), null, status, request);
  }

  @Override
  public void handleError(HttpContext request, @Nullable String message) throws IOException {
    request.send(error(request));
  }

}
