/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.sql.SQLException;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.util.BlogUtils;
import infra.beans.TypeMismatchException;
import infra.dao.DataAccessResourceFailureException;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageNotReadableException;
import infra.validation.ObjectError;
import infra.web.InternalServerException;
import infra.web.NotFoundHandler;
import infra.web.RequestContext;
import infra.web.ResponseStatusException;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestControllerAdvice;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.NotMultipartRequestException;
import infra.web.handler.ResponseEntityExceptionHandler;
import infra.web.handler.SimpleNotFoundHandler;
import infra.web.multipart.MaxUploadSizeExceededException;
import lombok.CustomLog;

/**
 * 异常处理
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-20 16:22
 */
@CustomLog
@RestControllerAdvice
public class ExceptionHandling extends ResponseEntityExceptionHandler implements NotFoundHandler {

  private static final ErrorMessage illegalArgument = ErrorMessage.failed("参数错误");

  private static final ErrorMessage internalServerError = ErrorMessage.failed("服务器内部异常");

  @Nullable
  @Override
  public Object handleNotFound(RequestContext request) {
    request.setStatus(HttpStatus.NOT_FOUND);

    SimpleNotFoundHandler.logNotFound(request);
    return ErrorMessage.failed("资源找不到");
  }

  @ExceptionHandler(ErrorMessageException.class)
  public ResponseEntity<ErrorMessage> errorMessage(ErrorMessageException errorMessage) {
    HttpStatusCode httpStatus = errorMessage.getStatusCode();
    return ResponseEntity.status(httpStatus)
            .body(ErrorMessage.failed(errorMessage.getMessage()));
  }

  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ExceptionHandler(UnauthorizedException.class)
  public ErrorMessage unauthorized() {
    return ErrorMessage.failed("登录超时");
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  public ErrorMessage illegalArgument() {
    return illegalArgument;
  }

  @ExceptionHandler(InternalServerException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorMessage internal(InternalServerException internal) {
    log.error("服务器内部错误", internal);
    return ErrorMessage.failed(internal.getReason());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> statusException(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
            .headers(e.getHeaders())
            .body(ErrorMessage.failed(e.getReason()));
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(ArticlePasswordException.class)
  public ErrorMessage articlePassword(ArticlePasswordException exception) {
    return ErrorMessage.failed(exception.getMessage());
  }

  @ExceptionHandler
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorMessage error(Throwable exception) {
    log.error("An Exception occurred", exception);
    if (exception instanceof SQLException) {
      return internalServerError;
    }
    return ErrorMessage.failed("服务器内部错误,稍后重试");
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(NullPointerException.class)
  public ErrorMessage nullPointer(NullPointerException exception) {
    log.error("Null Pointer occurred", exception);
    return internalServerError;
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(NotMultipartRequestException.class)
  public ErrorMessage notMultipart() {
    return ErrorMessage.failed("请求错误");
  }

  @ExceptionHandler({ OSSException.class, ClientException.class })
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorMessage ossException(Exception e) {
    return ErrorMessage.failed(e.getMessage());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(DataAccessResourceFailureException.class)
  public ErrorMessage dataAccessException(DataAccessResourceFailureException accessException) {
    log.error("数据库连接出错", accessException);
    return ErrorMessage.failed("数据库连接出错");
  }

  @Nullable
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
          HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    if (ex.hasErrors()) {
      ObjectError objectError = ex.getGlobalError();
      if (objectError == null) {
        objectError = ex.getFieldError();
      }
      if (objectError != null) {
        String defaultMessage = objectError.getDefaultMessage();
        return handleExceptionInternal(ex, ErrorMessage.failed(defaultMessage), headers, status, request);
      }
    }
    return handleExceptionInternal(ex, illegalArgument, headers, status, request);
  }

  @Nullable
  @Override
  protected ResponseEntity<Object> handleMissingRequestParameter(
          MissingRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    return handleExceptionInternal(ex, ErrorMessage.failed("缺少参数'" + ex.getParameterName() + "'"), headers, status, request);
  }

  @Nullable
  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
          MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    String formattedSize = BlogUtils.formatSize(ex.getMaxUploadSize());
    return handleExceptionInternal(ex, ErrorMessage.failed("上传文件大小超出限制: '" + formattedSize + "'"), headers, status, request);
  }

  @Nullable
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    return handleExceptionInternal(ex, ErrorMessage.failed("参数读取错误，请检查格式"), headers, status, request);
  }

  @Nullable
  @Override
  protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    return handleExceptionInternal(ex, ErrorMessage.failed("参数错误，请检查"), headers, status, request);
  }

}
