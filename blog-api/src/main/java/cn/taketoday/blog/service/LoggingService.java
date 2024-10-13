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

package cn.taketoday.blog.service;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.log.MethodOperation;
import cn.taketoday.blog.model.Operation;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.LoggingType;
import cn.taketoday.blog.util.IpSearchers;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.Json;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.ip2region.IpLocation;
import cn.taketoday.lang.Constant;
import cn.taketoday.persistence.EntityManager;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import lombok.CustomLog;

/**
 * 日志服务
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 18:38
 */
@Service
@CustomLog
public class LoggingService {

  private final EntityManager entityManager;

  private final LoggingExpressionEvaluator expressionEvaluator;

  public LoggingService(BeanFactory beanFactory, EntityManager entityManager) {
    this.expressionEvaluator = new LoggingExpressionEvaluator(beanFactory);
    this.entityManager = entityManager;
  }

  public void persist(Operation operation) {
    entityManager.persist(operation);
  }

  public void persist(MethodOperation operation) {
    try {
      Operation build = build(operation);
      try {
        persist(build);
      }
      catch (Throwable e) {
        log.error("保存日志的时候出现异常", e);
        afterThrowing(e, createErrorOperation());
      }
    }
    catch (Throwable e) {
      log.error("处理日志的时候出现异常", e);
      afterThrowing(e, createErrorOperation());
    }
  }

  protected Operation createErrorOperation() {
    Operation operation = new Operation();
    operation.setTitle("系统日志同步错误");
    operation.setUser("日志系统同步");
    return operation;
  }

  private Operation build(MethodOperation operation) {
    Operation entity = new Operation();

    Method method = operation.invocation.getMethod();
    MergedAnnotations annotations = MergedAnnotations.from(method);
    MergedAnnotation<Logging> logger = annotations.get(Logging.class);

    // get the user email
    User user = operation.loginUser;
    if (user == null) {
      entity.setUser("登录超时");
    }
    else {
      entity.setUser(user.getEmail());
    }

    Object result = operation.returnValue;
    String content = getContent(operation, logger, result);

    entity.setTitle(logger.getString("title"));
    entity.setIp(operation.ip);
    entity.setContent(content);
    entity.setInvokeAt(operation.invokeAt);

    IpLocation ipLocation = IpSearchers.find(operation.ip);
    if (ipLocation != null) {
      entity.setIpCountry(ipLocation.getCountry());
      entity.setIpProvince(ipLocation.getProvince());
      entity.setIpCity(ipLocation.getCity());
      entity.setIpArea(ipLocation.getArea());
      entity.setIpIsp(ipLocation.getIsp());
    }

    if (operation.afterThrowing) {
      resolveError(operation.throwable, entity);
    }
    else {
      entity.setType(LoggingType.SUCCESS);
    }

    // no exception occurred
    if (operation.returnValue instanceof Json json) {
      entity.setType(json.isSuccess() ? LoggingType.SUCCESS : LoggingType.ERROR);
    }
    return entity;
  }

  private String getContent(MethodOperation operation, MergedAnnotation<Logging> logger, Object result) {
    String content = logger.getString("content");
    if (!Constant.DEFAULT_NONE.equals(content)
            && StringUtils.hasText(content)) {
      try {
        return expressionEvaluator.content(content, operation, result);
      }
      catch (ExpressionException e) {
        log.error("不能执行EL表达式: [{}]", content, e);
        return "不能执行EL表达式: [" + content + "]";
      }
    }
    return "[没有内容]";
  }

  void resolveError(Throwable throwable, Operation entity) {
    entity.setType(LoggingType.ERROR);
    String content = entity.getContent();
    if (content == null) {
      entity.setContent("操作失败: " + throwable);
    }
    else {
      entity.setContent(content + "操作失败: " + throwable);
    }
  }

  public void afterThrowing(Throwable e, Operation errorOperation) {
    errorOperation.setIp("127.0.0.1");
    errorOperation.setType(LoggingType.ERROR);
    errorOperation.setContent("错误信息：" + e.getMessage());

    persist(errorOperation);
  }

  /**
   * 查询最新的五条日志
   *
   * @return List
   */
  public List<Operation> getLatest() {
    return entityManager.find(Operation.class, Queries.forSelect(select -> select.limit(5)
            .orderBy()
            .desc("id")));
  }

  public void truncateTable() {
    entityManager.truncate(Operation.class);
  }

  public void deleteById(long id) {
    entityManager.delete(Operation.class, id);
  }

  @Transactional
  public void deleteByIds(long[] idArray) {
    for (long id : idArray) {
      entityManager.delete(Operation.class, id);
    }
  }

  public List<Operation> getAll() {
    // select * FROM logging
    return entityManager.find(Operation.class);
  }

  public Pagination<Operation> pagination(Pageable pageable) {
    return Pagination.from(entityManager.page(Operation.class, pageable));
  }

  private static int pageNow(Pageable pageable) {
    return (pageable.pageNumber() - 1) * pageable.pageSize();
  }

}
