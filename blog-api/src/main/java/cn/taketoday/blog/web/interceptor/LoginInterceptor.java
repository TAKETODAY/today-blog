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

package cn.taketoday.blog.web.interceptor;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.model.User;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.annotation.RegisterReflection;
import infra.session.SessionManagerOperations;
import infra.web.HandlerInterceptor;
import infra.web.RequestContext;
import infra.web.resource.ResourceHttpRequestHandler;

/**
 * 登录拦截器，用于验证用户会话状态。
 * <p>
 * 如果用户未登录且请求的不是静态资源，则抛出 {@link UnauthorizedException} 异常。
 * 如果请求的是静态资源且用户未登录，则返回 404 状态码。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-30 20:36
 */
@RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
final class LoginInterceptor implements HandlerInterceptor {

  // Authorization

  private final SessionManagerOperations sessionManagerOperations;

  LoginInterceptor(SessionManagerOperations sessionManagerOperations) {
    this.sessionManagerOperations = sessionManagerOperations;
  }

  @Override
  public boolean preProcessing(RequestContext request, Object handler) throws Throwable {
    if (User.isPresent(sessionManagerOperations.getSession(request, false))) {
      return true;
    }

    if (handler instanceof ResourceHttpRequestHandler) {
      request.setStatus(404);
      request.getWriter().write("Not Found");
      return false;
    }
    throw new UnauthorizedException();
  }

}
