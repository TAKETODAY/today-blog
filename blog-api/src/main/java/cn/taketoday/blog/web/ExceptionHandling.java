/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.sql.SQLException;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.NotFoundHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ResponseStatusException;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.bind.resolver.ParameterConversionException;
import cn.taketoday.web.handler.ResponseEntityExceptionHandler;
import cn.taketoday.web.handler.SimpleNotFoundHandler;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;
import io.prometheus.client.Counter;
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

  static final Counter requests = Counter.build()
          .name("requests_not_found")
          .labelNames("uri")
          .help("Total Not Found requests.").register();

  @Nullable
  @Override
  public Object handleNotFound(RequestContext request) {
    requests.labels(request.getRequestURI())
            .inc();

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
  public Json nullPointer(NullPointerException exception) {
    log.error("Null Pointer occurred", exception);
    StackTraceElement[] stackTrace = exception.getStackTrace();
    if (ObjectUtils.isNotEmpty(stackTrace)) {
      return Json.failed("空指针", stackTrace[0]);
    }
    return Json.failed("空指针", "暂无堆栈信息");
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ParameterConversionException.class)
  public ErrorMessage conversion() {
    return ErrorMessage.failed("参数转换失败");
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

}
