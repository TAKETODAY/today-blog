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

package cn.taketoday.blog.config;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionManagerSupport;
import cn.taketoday.session.WebSession;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/12 21:47
 */
@Singleton
public class UserSessionResolver extends SessionManagerSupport {

  public UserSessionResolver(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Nullable
  public User getLoginUser() {
    return getLoginUser(RequestContextHolder.getRequired());
  }

  @Nullable
  public User getLoginUser(RequestContext request) {
    WebSession session = getSession(request, false);
    if (session != null) {
      Object attribute = session.getAttribute(BlogConstant.USER_INFO);
      if (attribute instanceof User loginUser) {
        return loginUser;
      }
    }
    return null;
  }

  @Nullable
  public Blogger getLoggedInBlogger() {
    return getLoggedInBlogger(RequestContextHolder.getRequired());
  }

  @Nullable
  public Blogger getLoggedInBlogger(RequestContext request) {
    WebSession session = getSession(request, false);
    if (session != null) {
      Object attribute = session.getAttribute(BlogConstant.BLOGGER_INFO);
      if (attribute instanceof Blogger blogger) {
        return blogger;
      }
    }
    return null;
  }

}
