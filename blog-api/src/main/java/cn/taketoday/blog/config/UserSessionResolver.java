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

package cn.taketoday.blog.config;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.service.ApiKeyService;
import cn.taketoday.blog.service.BloggerService;
import cn.taketoday.blog.web.LoginInfo;
import infra.session.Session;
import infra.session.SessionManagerOperations;
import infra.stereotype.Component;
import infra.web.HttpContext;
import infra.web.HttpContextHolder;

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
public class UserSessionResolver {

  private final SessionManagerOperations sessionManagerOperations;

  private final ApiKeyService apiKeyService;

  private final BloggerService bloggerService;

  public UserSessionResolver(SessionManagerOperations sessionManagerOperations,
          ApiKeyService apiKeyService, BloggerService bloggerService) {
    this.sessionManagerOperations = sessionManagerOperations;
    this.apiKeyService = apiKeyService;
    this.bloggerService = bloggerService;
  }

  /**
   * 解析当前请求身份。显式 API Key 优先于会话，认证结果只缓存在当前请求。
   */
  public LoginInfo resolve(HttpContext request) {
    Object cached = request.getAttribute(LoginInfo.class.getName());
    if (cached instanceof LoginInfo info) {
      return info;
    }
    LoginInfo info = new LoginInfo();
    if (usesApiKey(request)) {
      String value = ApiKeyService.extractValue(request.getHeader("Authorization"), request.getHeader("X-API-Key"));
      User user = apiKeyService.authenticate(value);
      if (user == null) {
        throw new UnauthorizedException("API Key 无效或已撤销");
      }
      info.setLoginUser(user);
      Blogger blogger = bloggerService.getBlogger();
      if (blogger != null && user.getEmail() != null && user.getEmail().equals(blogger.getEmail())) {
        info.setBlogger(blogger);
      }
    }
    else {
      Session session = sessionManagerOperations.getSession(request, false);
      if (session != null) {
        info.setLoginUser(User.find(session));
        info.setBlogger(Blogger.find(session));
      }
    }
    request.setAttribute(LoginInfo.class.getName(), info);
    return info;
  }

  public boolean usesApiKey(HttpContext request) {
    String authorization = request.getHeader("Authorization");
    return request.getHeader("X-API-Key") != null
            || (authorization != null && (authorization.equalsIgnoreCase("Bearer")
            || authorization.regionMatches(true, 0, "Bearer ", 0, 7)));
  }

  /**
   * 获取当前登录的用户
   */
  @Nullable
  public User getLoginUser() {
    return getLoginUser(HttpContextHolder.required());
  }

  /**
   * 获取当前登录的用户
   */
  @Nullable
  public User getLoginUser(HttpContext context) {
    return resolve(context).getLoginUser();
  }

  /**
   * 获取当前登录的博主
   */
  @Nullable
  public Blogger getLoggedInBlogger() {
    return getLoggedInBlogger(HttpContextHolder.required());
  }

  /**
   * 获取当前登录的博主
   */
  @Nullable
  public Blogger getLoggedInBlogger(HttpContext request) {
    return resolve(request).getBlogger();
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
  public Optional<User> loginUser(HttpContext request) {
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
  public Optional<Blogger> loggedInBlogger(HttpContext request) {
    return Optional.ofNullable(getLoggedInBlogger(request));
  }

}
