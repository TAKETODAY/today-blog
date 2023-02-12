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
import cn.taketoday.blog.MethodOperation;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.context.expression.AnnotatedElementKey;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.context.expression.CachedExpressionEvaluator;
import cn.taketoday.context.expression.MethodBasedEvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.common.TemplateParserContext;
import cn.taketoday.expression.spel.support.StandardTypeLocator;
import cn.taketoday.lang.NonNull;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/26 20:45
 */
public class LoggingExpressionEvaluator extends CachedExpressionEvaluator {
  static final ParserContext parserContext = new TemplateParserContext(
          "${", "}");

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

  @NonNull
  @Override
  protected Expression parseExpression(@NonNull String expression) {
    return getParser().parseExpression(expression, parserContext);
  }

  /**
   * Determine if the condition defined by the specified expression evaluates
   * to {@code true}.
   */
  public String content(String expression, MethodOperation methodOperation) {
    MethodInvocation invocation = methodOperation.getInvocation();

    var root = new LoggingRootObject(
            invocation.getMethod(), invocation.getArguments(),
            invocation.getThis(), invocation.getThis().getClass());

    var evaluationContext = new MethodBasedEvaluationContext(
            root, invocation.getMethod(), invocation.getArguments(), getParameterNameDiscoverer());

    evaluationContext.setBeanResolver(beanResolver);
    evaluationContext.setTypeLocator(typeLocator);

    var elementKey = new AnnotatedElementKey(root.method, root.targetClass);
    return getExpression(conditionCache, elementKey, expression)
            .getValue(evaluationContext, String.class);
  }

  public record LoggingRootObject(Method method, Object[] args, Object target, Class<?> targetClass) {

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

  }
}
