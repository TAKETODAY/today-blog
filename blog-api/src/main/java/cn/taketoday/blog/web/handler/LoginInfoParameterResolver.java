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

package cn.taketoday.blog.web.handler;

import org.jspecify.annotations.Nullable;

import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.UnauthorizedException;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.RequiresUser;
import infra.stereotype.Component;
import infra.web.HttpContext;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.handler.method.HandlerParameter;

/**
 * 登录信息参数解析
 * <p>
 * <ul>
 *   <li>{@code User 没有登录会抛异常}</li>
 *   <li>{@code Blogger 没有登录会抛异常}</li>
 *   <li>{@code LoginInfo 不会抛异常}</li>
 *   <li>{@code Optional<User> 不会抛异常}</li>
 *   <li>{@code Optional<Blogger> 不会抛异常}</li>
 *   <li>{@code @Nullable User 不会抛异常}</li>
 *   <li>{@code @Nullable Blogger 不会抛异常}</li>
 *   <li>{@code @RequiresUser User} 没有登录会抛异常</li>
 *   <li>{@code @RequiresUser Blogger 没有登录会抛异常}</li>
 *   <li>{@code @RequiresUser LoginInfo 没有登录会抛异常}</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-25 00:56
 */
@Component
class LoginInfoParameterResolver implements ParameterResolvingStrategy {

  private final UserSessionResolver identities;

  LoginInfoParameterResolver(UserSessionResolver identities) {
    this.identities = identities;
  }

  @Override
  public boolean supportsParameter(HandlerParameter parameter) {
    return parameter.is(User.class)
            || parameter.is(Blogger.class)
            || parameter.is(LoginInfo.class);
  }

  @Override
  public @Nullable Object resolveArgument(HttpContext context, HandlerParameter parameter) throws Exception {
    LoginInfo identity = identities.resolve(context);
    if (parameter.is(LoginInfo.class)) {
      if (parameter.hasParameterAnnotation(RequiresUser.class) && !identity.isLoggedIn()) {
        throw new UnauthorizedException();
      }
      return identity;
    }
    Object value = parameter.is(User.class) ? identity.getLoginUser() : identity.getBlogger();
    if (value != null || parameter.isNotRequired()) {
      return value;
    }
    throw new UnauthorizedException();
  }

}
