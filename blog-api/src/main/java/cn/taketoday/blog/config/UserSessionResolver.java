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

import java.util.Optional;

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
 * 关于 获取用户会话 的处理器
 * <p>
 * 可以获取当前登录用户，和登录的博主
 * </p>
 * 该类一般在控制层使用
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/12 21:47
 */
@Singleton
public class UserSessionResolver extends SessionManagerSupport {

  public UserSessionResolver(SessionManager sessionManager) {
    super(sessionManager);
  }

  /**
   * 获取当前登录的用户
   */
  @Nullable
  public User getLoginUser() {
    return getLoginUser(RequestContextHolder.getRequired());
  }

  /**
   * 获取当前登录的用户
   */
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

  /**
   * 获取当前登录的博主
   */
  @Nullable
  public Blogger getLoggedInBlogger() {
    return getLoggedInBlogger(RequestContextHolder.getRequired());
  }

  /**
   * 获取当前登录的博主
   */
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

  /**
   * 获取当前登录的用户
   */
  public Optional<User> loginUser() {
    return Optional.ofNullable(getLoginUser());
  }

  /**
   * 获取当前登录的用户
   */
  public Optional<User> loginUser(RequestContext request) {
    return Optional.ofNullable(getLoginUser(request));
  }

  /**
   * 获取当前登录的博主
   */
  public Optional<Blogger> loggedInBlogger() {
    return Optional.ofNullable(getLoggedInBlogger());
  }

  /**
   * 获取当前登录的博主
   */
  public Optional<Blogger> loggedInBlogger(RequestContext request) {
    return Optional.ofNullable(getLoggedInBlogger(request));
  }

}
