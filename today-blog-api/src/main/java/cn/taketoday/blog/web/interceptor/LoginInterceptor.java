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
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.UnauthorizedException;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-30 20:36
 */
public class LoginInterceptor extends SessionHandlerInterceptor {

  // Authorization

  public LoginInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public boolean beforeProcess(RequestContext request, Object handler) throws Throwable {
    if (getAttribute(request, BlogConstant.USER_INFO) != null) {
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
