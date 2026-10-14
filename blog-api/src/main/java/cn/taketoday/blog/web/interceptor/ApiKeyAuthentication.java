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

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.service.ApiKeyService;
import cn.taketoday.blog.service.BloggerService;
import infra.session.Session;
import infra.session.SessionManagerOperations;
import infra.web.HttpContext;

/**
 * 将 API Key 认证转换为现有的 Session 登录状态。
 */
final class ApiKeyAuthentication {

  private ApiKeyAuthentication() {
  }

  static boolean authenticate(HttpContext request, SessionManagerOperations sessions,
          ApiKeyService apiKeyService, BloggerService bloggerService) {
    String value = ApiKeyService.extractValue(request.getHeader("Authorization"), request.getHeader("X-API-Key"));
    User user = apiKeyService.authenticate(value);
    if (user == null) {
      return false;
    }

    Session session = sessions.getSession(request);
    user.bindTo(session);
    Blogger blogger = bloggerService.getBlogger();
    if (blogger != null && blogger.getEmail().equals(user.getEmail())) {
      blogger.bindTo(session);
      user.setBlogger(true);
    }
    return true;
  }
}
