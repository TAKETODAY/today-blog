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

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.problem.CommonProblems;
import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.problem.ProblemResponses;
import cn.taketoday.blog.util.BlogUtils;
import infra.beans.TypeMismatchException;
import infra.dao.DataAccessResourceFailureException;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageNotReadableException;
import infra.validation.FieldError;
import infra.web.ServerHttpExchange;
import infra.web.MultipartException;
import infra.web.NotFoundHandler;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.RestControllerAdvice;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.MissingRequestParameterException;
import infra.web.handler.ResponseEntityExceptionHandler;
import infra.web.server.InternalServerException;
import infra.web.server.NotMultipartRequestException;
import infra.web.server.ResponseStatusException;
import lombok.CustomLog;

/**
 * 异常处理
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-20 16:22
 */
@CustomLog
@RestControllerAdvice
class ExceptionHandling extends ResponseEntityExceptionHandler implements NotFoundHandler {

  private final ProblemResponses problems;

  ExceptionHandling(ProblemResponses problems) {
    this.problems = problems;
  }

  @Override
  public @Nullable Object handleNotFound(ServerHttpExchange request) {
    request.setStatus(HttpStatus.NOT_FOUND);
    if (pageNotFoundLogger.isWarnEnabled()) {
      pageNotFoundLogger.warn("No mapping for {} {}, [{}]",
              request.getMethodAsString(), request.getRequestURI(), BlogUtils.remoteAddress(request));
    }
    problems.prepare(request);
    return problems.create(CommonProblems.NOT_FOUND, request);
  }

  @ExceptionHandler(ProblemException.class)
  public ResponseEntity<ProblemDetail> problem(ProblemException exception, ServerHttpExchange request) {
    ProblemDetail problem = problems.createWithDetail(exception.getProblem(), request,
            exception.getDetail(), exception.getArguments());
    return problems.response(problem, exception.getHeaders(), exception.getStatusCode(), request);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ProblemDetail> unauthorized(ServerHttpExchange request) {
    return problems.response(CommonProblems.AUTH_REQUIRED, request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> illegalArgument(IllegalArgumentException e, ServerHttpExchange request) {
    log.error("Unexpected illegal argument", e);
    return problems.response(CommonProblems.ILLEGAL_ARGUMENT, request);
  }

  @ExceptionHandler(InternalServerException.class)
  public ResponseEntity<ProblemDetail> internal(InternalServerException internal, ServerHttpExchange request) {
    log.error("服务器内部错误", internal);
    return problems.response(CommonProblems.INTERNAL_ERROR, request);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> statusException(ResponseStatusException e, ServerHttpExchange request) {
    if (e.getStatusCode().value() >= 500) {
      log.error("Request failed", e);
    }
    return problems.response(problems.create(CommonProblems.forStatus(e.getStatusCode()), request),
            e.getHeaders(), e.getStatusCode(), request);
  }

  @ExceptionHandler
  public ResponseEntity<ProblemDetail> error(Throwable exception, ServerHttpExchange request) {
    log.error("An Exception occurred", exception);
    return problems.response(CommonProblems.INTERNAL_ERROR, request);
  }

  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<ProblemDetail> nullPointer(NullPointerException exception, ServerHttpExchange request) {
    log.error("Null Pointer occurred", exception);
    return problems.response(CommonProblems.INTERNAL_ERROR, request);
  }

  @ExceptionHandler(NotMultipartRequestException.class)
  public ResponseEntity<ProblemDetail> notMultipart(ServerHttpExchange request) {
    return problems.response(CommonProblems.INVALID_REQUEST, request);
  }

  @ExceptionHandler({ OSSException.class, ClientException.class })
  public ResponseEntity<ProblemDetail> ossException(Exception e, ServerHttpExchange request) {
    log.error("Object storage request failed", e);
    return problems.response(CommonProblems.INTERNAL_ERROR, request);
  }

  @ExceptionHandler(DataAccessResourceFailureException.class)
  public ResponseEntity<ProblemDetail> dataAccessException(DataAccessResourceFailureException accessException, ServerHttpExchange request) {
    log.error("数据库连接出错", accessException);
    return problems.response(CommonProblems.INTERNAL_ERROR, request);
  }

  @Override
  protected @Nullable ResponseEntity<@Nullable Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
          HttpHeaders headers, HttpStatusCode status, ServerHttpExchange request) {
    ProblemDetail problem = problems.create(CommonProblems.VALIDATION_FAILED, request);
    problem.setProperty("errors", ex.getAllErrors().stream().map(error -> {
      Map<String, @Nullable Object> item = new LinkedHashMap<>();
      if (error instanceof FieldError field) {
        item.put("field", field.getField());
      }
      item.put("code", error.getCode());
      item.put("detail", problems.validationMessage(error, request));
      return item;
    }).toList());
    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<@Nullable Object> handleMissingRequestParameter(
          MissingRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, ServerHttpExchange request) {
    return handleExceptionInternal(ex, problems.create(CommonProblems.PARAMETER_MISSING, request,
            ex.getParameterName()), headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<@Nullable Object> handleMultipartException(MultipartException ex, HttpHeaders headers, HttpStatusCode status, ServerHttpExchange request) {
    return handleExceptionInternal(ex, problems.create(CommonProblems.forStatus(status), request), headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<@Nullable Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, ServerHttpExchange request) {
    return handleExceptionInternal(ex, problems.create(CommonProblems.INVALID_REQUEST, request), headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<@Nullable Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, ServerHttpExchange request) {
    return handleExceptionInternal(ex, problems.create(CommonProblems.INVALID_REQUEST, request), headers, status, request);
  }

  @Override
  protected ResponseEntity<@Nullable Object> createResponseEntity(@Nullable Object body,
          @Nullable HttpHeaders headers, HttpStatusCode status, ServerHttpExchange request) {
    ProblemDetail problem;
    if (body instanceof ProblemDetail candidate && candidate.getProperty("code") != null) {
      problem = candidate;
    }
    else {
      problem = problems.create(CommonProblems.forStatus(status), request);
    }
    problem.setStatus(status.value());
    problems.prepare(request);
    return ResponseEntity.status(status)
            .headers(headers)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
  }

}
