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

package cn.taketoday.blog.web.interceptor;

import java.time.Clock;

import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.ErrorMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionRepository;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:38
 */
public class BloggerInterceptor extends SessionHandlerInterceptor {

  private final Clock clock = Clock.systemUTC();

  private final SessionRepository repository;

  public BloggerInterceptor(SessionManager sessionManager, SessionRepository repository) {
    super(sessionManager);
    this.repository = repository;
  }

  @Override
  public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    WebSession session = getSession(request, false);
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
