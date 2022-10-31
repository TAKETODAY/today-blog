/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.blog.aspect;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.ThreadPoolExecutor;

import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.service.LoggingService;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;

import static cn.taketoday.blog.utils.BlogUtils.remoteAddress;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-06 17:52
 */
public class LoggingInterceptor implements MethodInterceptor {

  private final ThreadPoolExecutor executor;
  private final LoggingService loggingService;
  private final UserSessionResolver sessionResolver;

  public LoggingInterceptor(ThreadPoolExecutor executor,
          LoggingService loggingService, UserSessionResolver sessionResolver) {
    this.executor = executor;
    this.loggingService = loggingService;
    this.sessionResolver = sessionResolver;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object result = null;
    try {
      return result = invocation.proceed();
    }
    catch (Throwable e) {
      result = e;
      throw e;
    }
    finally {
      RequestContext request = RequestContextHolder.getRequired();
      LoggerOperationDetail operation =
              new LoggerOperationDetail(
                      remoteAddress(request),
                      result,
                      invocation,
                      loginUser(request)
              );

      executor.execute(() -> loggingService.save(operation));
    }
  }

  @Nullable
  private User loginUser(RequestContext request) {
    return sessionResolver.getLoginUser(request);
  }

}
