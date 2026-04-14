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

import org.jspecify.annotations.Nullable;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.ErrorMessage;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.annotation.RegisterReflection;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.session.Session;
import infra.session.SessionManagerOperations;
import infra.session.SessionRepository;
import infra.web.HandlerInterceptor;
import infra.web.InterceptorChain;
import infra.web.RequestContext;
import infra.web.resource.ResourceHttpRequestHandler;

/**
 * 博主拦截器，用于验证请求是否来自已登录的博主。
 * <p>
 * 该拦截器检查会话中是否存在有效的用户信息以及是否为博主身份。
 * 如果是博主，则更新最后访问时间并继续处理请求；
 * 如果是普通用户但非博主，则返回 404 Not Found；
 * 如果未登录且访问的不是静态资源，则抛出未授权异常。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:38
 */
@RegisterReflection(memberCategories = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
final class BloggerInterceptor implements HandlerInterceptor {

  private final SessionRepository repository;

  private final SessionManagerOperations sessionManagerOperations;

  BloggerInterceptor(SessionRepository repository, SessionManagerOperations sessionManagerOperations) {
    this.repository = repository;
    this.sessionManagerOperations = sessionManagerOperations;
  }

  @Override
  public @Nullable Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    Session session = sessionManagerOperations.getSession(request, false);
    if (session != null) {
      if (User.isPresent(session)) {
        if (Blogger.isPresent(session)) {
          repository.updateLastAccessTime(session);
          return chain.proceed(request);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorMessage.failed("Not Found"));
      }
    }
    if (chain.unwrapHandler() instanceof ResourceHttpRequestHandler) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body("Not Found");
    }
    return ErrorMessage.unauthorized;
  }

}
