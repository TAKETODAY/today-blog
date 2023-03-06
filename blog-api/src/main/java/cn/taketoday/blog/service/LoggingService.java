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
import cn.taketoday.blog.Json;
import cn.taketoday.blog.MethodOperation;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.aspect.Logging;
import cn.taketoday.blog.model.Operation;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.LoggingType;
import cn.taketoday.blog.util.IpUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.persistence.EntityManager;
import cn.taketoday.stereotype.Service;
import lombok.CustomLog;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 18:38
 */
@Service
@CustomLog
public class LoggingService {

  private final EntityManager entityManager;
  private final RepositoryManager repositoryManager;
  private final LoggingExpressionEvaluator expressionEvaluator;

  public LoggingService(BeanFactory beanFactory, RepositoryManager repositoryManager) {
    this.expressionEvaluator = new LoggingExpressionEvaluator(beanFactory);
    this.entityManager = repositoryManager.getEntityManager();
    this.repositoryManager = repositoryManager;
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
      operation.setType(LoggingType.SUCCESS.getValue());
    }

    // no exception occurred
    if (result instanceof Json json) {
      operation.setResult(json.getMessage());
      operation.setType(json.isSuccess() ? LoggingType.SUCCESS.getValue() : LoggingType.ERROR.getValue());
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

  private String getContent(MethodOperation operation, MergedAnnotation<Logging> logger) {
    String contentTemplate = logger.getString("content");
    try {
      return expressionEvaluator.content(contentTemplate, operation);
    }
    catch (ExpressionException e) {
      log.error("不能执行EL表达式: [{}]", contentTemplate, e);
      return "不能执行EL表达式: [" + contentTemplate + "]";
    }
  }

  void resolveError(Throwable throwable, Operation operation) {
    operation.setType(LoggingType.ERROR.getValue());
    String content = operation.getContent();
    if (content == null) {
      operation.setContent("msg: <em>操作失败: </em>" + throwable);
    }
    else {
      operation.setContent("msg: <em>" + content + "操作失败: </em>" + throwable);
    }
  }

  public void afterThrowing(Throwable e, Operation errorOperation) {

    persist(errorOperation.setIp("127.0.0.1:local")//
            .setId(System.currentTimeMillis())//
            .setType(LoggingType.ERROR.getValue())//
            .setContent("错误信息：" + e.getMessage()));
  }

  /**
   * 查询最新的五条日志
   *
   * @return List
   */
  public List<Operation> getLatest() {
    try (Query query = repositoryManager.createQuery("SELECT * FROM logger ORDER BY id DESC LIMIT 0, 5")) {
      return query.fetch(Operation.class);
    }
  }

  public int count() {
    try (Query query = repositoryManager.createQuery("SELECT COUNT(*) FROM logger")) {
      return query.fetchScalar(int.class);
    }
  }

  public List<Operation> getAll(int page, int size) {
    try (NamedQuery query = repositoryManager.createNamedQuery(
            "SELECT * FROM logger ORDER BY id DESC LIMIT :pageNow, :pageSize")) {
      // language=
      query.addParameter("pageNow", (page - 1) * size);
      query.addParameter("pageSize", size);
      return query.fetch(Operation.class);
    }
  }

  public void deleteAll() {
    try (Query query = repositoryManager.createQuery("truncate table logger")) {
      query.executeUpdate(false);
    }
  }

  public void deleteById(long id) {
    entityManager.delete(Operation.class, id);
  }

  public void deleteByIds(long[] id) {
    try (var query = repositoryManager.createNamedQuery("delete from logger where id IN (:id)")) {
      query.addParameter("id", id);
      query.executeUpdate(false);
    }
  }

  public List<Operation> getAll() {
    // select * FROM logger
    return entityManager.find(Operation.class);
  }

  public Pagination<Operation> pagination(Pageable pageable) {
    try (JdbcConnection connection = repositoryManager.open()) {
      try (Query countQuery = connection.createQuery("SELECT COUNT(*) FROM logger")) {
        int count = countQuery.fetchScalar(int.class);
        try (NamedQuery dataQuery = connection.createNamedQuery(
                "SELECT * FROM logger ORDER BY id DESC LIMIT :pageNow, :pageSize")) {
          // language=
          dataQuery.addParameter("pageNow", pageNow(pageable));
          dataQuery.addParameter("pageSize", pageable.getSize());
          List<Operation> all = dataQuery.fetch(Operation.class);
          return Pagination.ok(all, count, pageable);
        }
      }
    }
  }

  private static int pageNow(Pageable pageable) {
    return (pageable.getCurrent() - 1) * pageable.getSize();
  }

}
