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

package cn.taketoday.blog.config;

import java.util.Optional;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import infra.lang.Nullable;
import infra.session.SessionManager;
import infra.session.SessionManagerOperations;
import infra.session.WebSession;
import infra.stereotype.Component;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;

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
@Component
public class UserSessionResolver extends SessionManagerOperations {

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
      return User.find(session);
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
      return Blogger.find(session);
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
