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

package cn.taketoday.blog.web.interceptor;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ErrorMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.UnauthorizedException;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:38
 */
public class BloggerInterceptor extends SessionHandlerInterceptor {

  public BloggerInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    WebSession session = getSession(request);
    if (session.getAttribute(BlogConstant.USER_INFO) != null) {
      if (session.getAttribute(BlogConstant.BLOGGER_INFO) != null) {
        return chain.proceed(request);
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .contentType(MediaType.APPLICATION_JSON)
              .body(ErrorMessage.failed("Not Found"));
    }
    if (chain.getHandler() instanceof ResourceHttpRequestHandler) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body("Not Found");
    }
    throw new UnauthorizedException();
  }

}
