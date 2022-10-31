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
import cn.taketoday.expression.spel.support.StandardTypeLocator;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/26 20:45
 */
public class LoggingExpressionEvaluator extends CachedExpressionEvaluator {

  private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);
  private final StandardTypeLocator typeLocator = new StandardTypeLocator();

  @Nullable
  private final BeanFactory beanFactory;

  public LoggingExpressionEvaluator(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    typeLocator.importClass(Arrays.class);
    typeLocator.importClass(UserStatus.class);
    typeLocator.importClass(PostStatus.class);
    typeLocator.importClass(CommentStatus.class);
  }

  /**
   * Determine if the condition defined by the specified expression evaluates
   * to {@code true}.
   */
  public String content(String expression, MethodOperation methodOperation) {
    MethodInvocation invocation = methodOperation.getInvocation();

    LoggingRootObject root = new LoggingRootObject(
            invocation.getMethod(), invocation.getArguments(), invocation.getThis(), invocation.getThis().getClass());

    MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
            root, invocation.getMethod(), invocation.getArguments(), getParameterNameDiscoverer());

    if (beanFactory != null) {
      evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
    }

    evaluationContext.setTypeLocator(typeLocator);
    AnnotatedElementKey elementKey = new AnnotatedElementKey(root.method, root.targetClass);
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
