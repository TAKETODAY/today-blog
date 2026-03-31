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

package cn.taketoday.blog.log;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.model.IpLocation;
import cn.taketoday.blog.model.OperationLogging;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.LoggingType;
import cn.taketoday.blog.service.IpLocationService;
import cn.taketoday.blog.util.StringUtils;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Role;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.expression.ExpressionException;
import infra.lang.Constant;
import infra.stereotype.Component;
import infra.util.concurrent.Future;
import infra.util.function.SingletonSupplier;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import lombok.CustomLog;

import static cn.taketoday.blog.util.BlogUtils.remoteAddress;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-06 17:52
 */
@CustomLog
@Component("loggingInterceptor")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
final class LoggingInterceptor implements MethodInterceptor {

  private final SingletonSupplier<Executor> executor;

  private final ObjectProvider<LoggingPersister> loggingPersister;

  private final SingletonSupplier<UserSessionResolver> sessionResolver;

  private final LoggingExpressionEvaluator expressionEvaluator;

  private final IpLocationService ipLocationService;

  public LoggingInterceptor(BeanFactory beanFactory,
          IpLocationService ipLocationService,
          ObjectProvider<Executor> executor,
          ObjectProvider<LoggingPersister> loggingPersister,
          ObjectProvider<UserSessionResolver> sessionResolver) {

    this.loggingPersister = loggingPersister;
    this.ipLocationService = ipLocationService;
    this.executor = SingletonSupplier.of(executor);
    this.sessionResolver = SingletonSupplier.of(sessionResolver);
    this.expressionEvaluator = new LoggingExpressionEvaluator(beanFactory);
  }

  @Override
  public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
    Object result = null;
    Throwable throwable = null;
    try {
      return result = invocation.proceed();
    }
    catch (Throwable e) {
      throwable = e;
      throw e;
    }
    finally {
      RequestContext request = RequestContextHolder.getRequired();
      MethodOperation operation = new MethodOperation(remoteAddress(request), invocation, loginUser(request));
      afterInvocation(operation, throwable, result);
    }
  }

  private void afterInvocation(MethodOperation operation, @Nullable Throwable throwable, @Nullable Object result) {
    executor.obtain().execute(() -> {
      if (result instanceof Future<?> future) {
        future.onCompleted(completed -> {
          if (completed.isSuccess()) {
            Object returnValue = completed.getNow();
            persist(operation, null, returnValue);
          }
          else {
            persist(operation, completed.getCause(), null);
          }
        });
      }
      else {
        persist(operation, throwable, result);
      }
    });
  }

  private @Nullable User loginUser(RequestContext request) {
    return sessionResolver.obtain().getLoginUser(request);
  }

  private void persist(MethodOperation operation, @Nullable Throwable error, @Nullable Object result) {
    try {
      OperationLogging entity = createEntity(operation, error, result);
      try {
        persist(entity);
      }
      catch (Throwable e) {
        log.error("保存日志的时候出现异常", e);
        afterThrowing(e, createErrorEntity());
      }
    }
    catch (Throwable e) {
      log.error("处理日志的时候出现异常", e);
      afterThrowing(e, createErrorEntity());
    }
  }

  private OperationLogging createErrorEntity() {
    OperationLogging operation = new OperationLogging();
    operation.setTitle("系统日志同步错误");
    operation.setUser("日志系统同步");
    return operation;
  }

  private OperationLogging createEntity(MethodOperation operation, @Nullable Throwable error, @Nullable Object returnValue) {
    MergedAnnotation<Logging> logging = MergedAnnotations.from(operation.invocation.getMethod()).get(Logging.class);
    OperationLogging entity = new OperationLogging();

    // get the user email
    User user = operation.loginUser;
    if (user == null) {
      entity.setUser("未登录");
    }
    else {
      entity.setUser(user.getEmail());
    }

    String content = getContent(operation, logging, returnValue);

    entity.setTitle(logging.getString("title"));
    entity.setIp(operation.ip);
    entity.setContent(content);
    entity.setInvokeAt(operation.invokeAt);

    IpLocation ipLocation = ipLocationService.lookup(operation.ip);
    if (ipLocation != null) {
      entity.setIpCountry(ipLocation.getCountry());
      entity.setIpProvince(ipLocation.getProvince());
      entity.setIpCity(ipLocation.getCity());
      entity.setIpArea(ipLocation.getArea());
      entity.setIpIsp(ipLocation.getIsp());
    }

    if (error != null) {
      resolveError(error, entity);
    }
    else {
      entity.setType(LoggingType.SUCCESS);
    }
    return entity;
  }

  private String getContent(MethodOperation operation, MergedAnnotation<Logging> logging, @Nullable Object result) {
    String content = logging.getString("content");
    if (!Constant.DEFAULT_NONE.equals(content) && StringUtils.hasText(content)) {
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

  private void resolveError(Throwable throwable, OperationLogging entity) {
    entity.setType(LoggingType.ERROR);
    String content = entity.getContent();
    if (content == null) {
      entity.setContent("操作失败: " + throwable);
    }
    else {
      entity.setContent(content + "操作失败: " + throwable);
    }
  }

  private void afterThrowing(Throwable e, OperationLogging errorOperation) {
    errorOperation.setIp("127.0.0.1");
    errorOperation.setType(LoggingType.ERROR);
    errorOperation.setContent("错误信息：" + e.getMessage());

    persist(errorOperation);
  }

  private void persist(OperationLogging operation) {
    loggingPersister.get().persist(operation);
  }

}
