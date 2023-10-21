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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.blog.log.MethodOperation;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.context.expression.CachedExpressionEvaluator;
import cn.taketoday.context.expression.MethodBasedEvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.spel.support.StandardTypeLocator;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/26 20:45
 */
class LoggingExpressionEvaluator extends CachedExpressionEvaluator {
  static final ParserContext parserContext = ParserContext.TEMPLATE_EXPRESSION;

  /**
   * The name of the variable holding the result object.
   */
  public static final String RESULT_VARIABLE = "result";

  private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);
  private final StandardTypeLocator typeLocator = new StandardTypeLocator();
  private final BeanFactoryResolver beanResolver;

  public LoggingExpressionEvaluator(BeanFactory beanFactory) {
    this.beanResolver = new BeanFactoryResolver(beanFactory);
    typeLocator.importClass(Arrays.class);
    typeLocator.importClass(UserStatus.class);
    typeLocator.importClass(PostStatus.class);
    typeLocator.importClass(CommentStatus.class);
  }

  @Override
  protected Expression parseExpression(String expression) {
    return parser.parseExpression(expression, parserContext);
  }

  /**
   * 使用 SpEL 定制日志内容
   */
  public String content(String expression, MethodOperation operation, @Nullable Object result) {
    MethodInvocation invocation = operation.invocation;
    var root = new LoggingRootObject(
            operation, invocation.getMethod(), invocation.getArguments(),
            invocation.getThis(), invocation.getThis().getClass(), result);

    var evaluationContext = new MethodBasedEvaluationContext(
            root, invocation.getMethod(), invocation.getArguments(), parameterNameDiscoverer);

    evaluationContext.setBeanResolver(beanResolver);
    evaluationContext.setTypeLocator(typeLocator);

    var elementKey = new AnnotatedElementKey(root.method, root.targetClass);
    return getExpression(conditionCache, elementKey, expression)
            .getValue(evaluationContext, String.class);
  }

  public record LoggingRootObject(
          MethodOperation operation, Method method, Object[] args,
          Object target, Class<?> targetClass, @Nullable Object result) {

    public Method getMethod() {
      return this.method;
    }

    public String getMethodName() {
      return this.method.getName();
    }

    public Object[] getArgs() {
      return this.args;
    }

    public Object getTarget() {
      return this.target;
    }

    public Class<?> getTargetClass() {
      return this.targetClass;
    }

    public MethodOperation getOperation() {
      return operation;
    }

    @Nullable
    public Object getResult() {
      return result;
    }

  }
}
