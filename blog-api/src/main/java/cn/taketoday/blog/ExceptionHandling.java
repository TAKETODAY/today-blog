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

package cn.taketoday.blog;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;

import java.io.IOException;
import java.sql.SQLException;

import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.web.ArticlePasswordException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.BadRequestException;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.NotFoundException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ResponseStatusException;
import cn.taketoday.web.UnauthorizedException;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.NotMultipartRequestException;
import cn.taketoday.web.bind.resolver.ParameterConversionException;
import cn.taketoday.web.bind.resolver.ParameterReadFailedException;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;
import lombok.CustomLog;

/**
 * 异常处理
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-20 16:22
 */
@CustomLog
@RestControllerAdvice
public class ExceptionHandling {

  private static final ErrorMessage illegalArgument = ErrorMessage.failed("参数错误");
  private static final ErrorMessage internalServerError = ErrorMessage.failed("服务器内部异常");

  @ExceptionHandler(ErrorMessageException.class)
  public ResponseEntity<ErrorMessage> errorMessage(ErrorMessageException errorMessage) {
    HttpStatus httpStatus = errorMessage.getStatus();
    return ResponseEntity.status(httpStatus)
            .body(ErrorMessage.failed(errorMessage.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public void unauthorized(RequestContext request) throws IOException {
    request.setContentType(MediaType.APPLICATION_JSON_VALUE);
    request.getWriter().write(BlogConstant.NONE_LOGIN_MSG);
    // request.responseHeader(Constant.WWW_AUTHENTICATE, "Basic realm=taketoday.cn");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ErrorMessage illegalArgument() {
    return illegalArgument;
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public ErrorMessage notFoundException(NotFoundException exception) {
    return ErrorMessage.failed(exception.getReason());
  }

  @ExceptionHandler({ MaxUploadSizeExceededException.class })
  public ErrorMessage maxUploadSizeExceeded(MaxUploadSizeExceededException e) {
    log.error(e.getMessage(), e);
    String formattedSize = BlogUtils.formatSize(e.getMaxUploadSize());
    return ErrorMessage.failed("上传文件大小超出限制: '" + formattedSize + "'");
  }

  @ExceptionHandler(BadRequestException.class)
  public ErrorMessage badRequest(BadRequestException e) {
    return e.getCause() instanceof NumberFormatException
           ? ErrorMessage.failed("数字格式错误")
           : ErrorMessage.failed(e.getReason());
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
  public ErrorMessage error(Throwable exception) {
    log.error("An Exception occurred", exception);
    if (exception instanceof SQLException) {
      return internalServerError;
    }
    return ErrorMessage.failed("服务器内部错误,稍后重试");
  }

  @ExceptionHandler(NullPointerException.class)
  public Json nullPointer(NullPointerException exception) {
    log.error("Null Pointer occurred", exception);
    StackTraceElement[] stackTrace = exception.getStackTrace();
    if (ObjectUtils.isNotEmpty(stackTrace)) {
      return Json.failed("空指针", stackTrace[0]);
    }
    return Json.failed("空指针", "暂无堆栈信息");
  }

  @ExceptionHandler(ParameterConversionException.class)
  public ErrorMessage conversion() {
    return ErrorMessage.failed("参数转换失败");
  }

  @ExceptionHandler(MissingRequestParameterException.class)
  public ErrorMessage missingParameter(MissingRequestParameterException parameterException) {
    return ErrorMessage.failed("缺少参数'" + parameterException.getParameterName() + "'");
  }

  @ExceptionHandler(ParameterReadFailedException.class)
  public ErrorMessage parameterReadFailed(ParameterReadFailedException exception) {
    log.error("参数读取错误", exception);
    return ErrorMessage.failed("参数读取错误");
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

}
