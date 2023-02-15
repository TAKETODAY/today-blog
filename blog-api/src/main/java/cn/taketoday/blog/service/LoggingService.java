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
package cn.taketoday.blog.service;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.blog.MethodOperation;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.aspect.Logging;
import cn.taketoday.blog.model.Operation;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.LoggerType;
import cn.taketoday.blog.repository.LoggerRepository;
import cn.taketoday.blog.utils.IpUtils;
import cn.taketoday.blog.utils.Json;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.stereotype.Service;
import lombok.CustomLog;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 18:38
 */
@Service
@CustomLog
public class LoggingService {

  private final LoggerRepository loggerRepository;
  private final LoggingExpressionEvaluator expressionEvaluator;

  public LoggingService(LoggerRepository loggerRepository, BeanFactory beanFactory) {
    this.loggerRepository = loggerRepository;
    this.expressionEvaluator = new LoggingExpressionEvaluator(beanFactory);
  }

  public void save(Operation operation) {
    loggerRepository.save(operation);
  }

  public void save(MethodOperation operation) {
    try {
      Operation build = build(operation);
      try {
        save(build);
      }
      catch (Throwable e) {
        log.error("保存日志的时候出现异常", e);
        afterThrowing(e, createErrorOperation(e));
      }
    }
    catch (Throwable e) {
      log.error("处理日志的时候出现异常", e);
      afterThrowing(e, createErrorOperation(e));
    }
  }

  protected Operation createErrorOperation(Throwable e) {
    return new Operation()
            .setResult("暂无")
            .setUser("日志系统同步")
            .setTitle("系统日志同步错误");
  }

  private Operation build(MethodOperation operationDetail) {
    Operation operation = new Operation();
    MethodInvocation invocation = operationDetail.getInvocation();

    Method method = invocation.getMethod();
    MergedAnnotations annotations = MergedAnnotations.from(method);
    MergedAnnotation<Logging> logger = annotations.get(Logging.class);

    // get the user email
    User user = operationDetail.getUser();
    if (user == null) {
      operation.setUser("登录超时");
    }
    else {
      operation.setUser(user.getEmail());
    }
    String content = getContent(operationDetail, logger);
    String ip = operationDetail.getIp();

    operation.setTitle(logger.getString("title"))
            .setContent(content)
            .setId(operationDetail.getId())
            .setIp(ip + ':' + IpUtils.search(ip));

    Object result = operationDetail.getResult();

    if (result instanceof Throwable e) {
      resolveError(e, operation);
    }
    else {
      operation.setType(LoggerType.SUCCESS.getType());
    }

    // no exception occurred
    if (result instanceof Json json) {
      operation.setResult(json.getMessage());
      operation.setType(json.isSuccess() ? LoggerType.SUCCESS.getType() : LoggerType.ERROR.getType());
    }
    else if (result instanceof String str) {
      String decodeUrl = URLDecoder.decode(str, StandardCharsets.UTF_8);
      operation.setResult(decodeUrl);
    }
    else if (result != null) {
      operation.setResult(result.toString());
    }
    return operation;
  }

  private String getContent(MethodOperation operationDetail, MergedAnnotation<Logging> logger) {
    try {
      return expressionEvaluator.content(logger.getString("content"), operationDetail);
    }
    catch (ExpressionException e) {
      throw new ExpressionException(
              "不能执行EL表达式: [" + logger.getString("content") + "]", e);
    }
  }

  void resolveError(Throwable throwable, Operation operation) {
    operation.setType(LoggerType.ERROR.getType());
    String content = operation.getContent();
    if (content == null) {
      operation.setContent("msg: <em>操作失败: </em>" + throwable);
    }
    else {
      operation.setContent("msg: <em>" + content + "操作失败: </em>" + throwable);
    }
  }

  public void afterThrowing(Throwable e, Operation errorOperation) {

    save(errorOperation.setIp("127.0.0.1:local")//
            .setId(System.currentTimeMillis())//
            .setType(LoggerType.ERROR.getType())//
            .setContent("错误信息：" + e.getMessage()));
  }

  /**
   * 查询最新的五条日志
   *
   * @return List
   */

  public List<Operation> getLatest() {
    return loggerRepository.findLatest();
  }

  public int count() {
    return loggerRepository.getTotalRecord();
  }

  public List<Operation> getAll(int page, int size) {
    return loggerRepository.find((page - 1) * size, size);
  }

  public void deleteAll() {
    loggerRepository.deleteAll();
  }

  public void deleteById(long id) {
    loggerRepository.deleteById(id);
  }

  public void deleteByIds(long[] id) {
    loggerRepository.deleteByIds(id);
  }

  public List<Operation> getAll() {
    return loggerRepository.findAll();
  }

  public Pagination<Operation> pagination(Pageable pageable) {
    int count = count();
    List<Operation> all = getAll(pageable);
    return Pagination.ok(all, count, pageable);
  }

  public List<Operation> getAll(Pageable pageable) {
    return getAll(pageable.getCurrent(), pageable.getSize());
  }

}
