/*
 * Copyright 2017 - 2025 the original author or authors.
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

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.ErrorMessage;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.session.SessionManagerOperations;
import infra.session.SessionRepository;
import infra.session.WebSession;
import infra.web.HandlerInterceptor;
import infra.web.InterceptorChain;
import infra.web.RequestContext;
import infra.web.resource.ResourceHttpRequestHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:38
 */
final class BloggerInterceptor implements HandlerInterceptor {

  private final SessionRepository repository;

  private final SessionManagerOperations sessionManagerOperations;

  BloggerInterceptor(SessionRepository repository, SessionManagerOperations sessionManagerOperations) {
    this.repository = repository;
    this.sessionManagerOperations = sessionManagerOperations;
  }

  @Nullable
  @Override
  public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    WebSession session = sessionManagerOperations.getSession(request, false);
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
    throw new UnauthorizedException();
  }

}
