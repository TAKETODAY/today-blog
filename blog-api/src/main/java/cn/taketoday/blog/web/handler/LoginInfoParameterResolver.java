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
package cn.taketoday.blog.web.handler;

import java.util.Optional;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.controller.RequiresUser;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionManagerSupport;
import cn.taketoday.session.WebSession;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.UnauthorizedException;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-25 00:56
 */
@Singleton
public class LoginInfoParameterResolver extends SessionManagerSupport implements ParameterResolvingStrategy {

  public LoginInfoParameterResolver(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    if (parameter.is(Optional.class)) {
      ResolvableMethodParameter nested = parameter.nested();
      return nested.is(User.class)
              || nested.is(Blogger.class);
    }

    return parameter.is(User.class)
            || parameter.is(Blogger.class)
            || parameter.is(LoginInfo.class);
  }

  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter parameter) {
    WebSession session = getSession(context, false);
    if (session != null) {

      if (parameter.is(Optional.class)) {
        if (parameter.is(User.class)) {
          return Optional.ofNullable(getAttribute(session, User.class, BlogConstant.USER_INFO));
        }
        return Optional.ofNullable(getAttribute(session, Blogger.class, BlogConstant.BLOGGER_INFO));
      }

      if (parameter.is(User.class)) {
        return getAttribute(parameter, session, User.class, BlogConstant.USER_INFO);
      }
      else if (parameter.is(Blogger.class)) {
        return getAttribute(parameter, session, Blogger.class, BlogConstant.BLOGGER_INFO);
      }
      else {
        LoginInfo info = new LoginInfo();
        Object attribute = session.getAttribute(BlogConstant.USER_INFO);
        if (attribute instanceof User loginUser) {
          info.setLoginUser(loginUser);
        }
        else if (parameter.hasParameterAnnotation(RequiresUser.class)) {
          throw new UnauthorizedException();
        }
        attribute = session.getAttribute(BlogConstant.BLOGGER_INFO);
        if (attribute instanceof Blogger blogger) {
          info.setBlogger(blogger);
        }

        return info;
      }
    }

    // session is null

    if (parameter.is(Optional.class)) {
      return Optional.empty();
    }

    if (parameter.is(User.class) || parameter.is(Blogger.class)) {
      if (parameter.isNotRequired()) {
        return null;
      }
      throw new UnauthorizedException();
    }

    if (parameter.hasParameterAnnotation(RequiresUser.class)) {
      throw new UnauthorizedException();
    }

    return new LoginInfo();
  }

  @Nullable
  private static Object getAttribute(
          WebSession session, Class<?> targetType, String key) {
    Object attribute = session.getAttribute(key);
    if (targetType.isInstance(attribute)) {
      return attribute;
    }
    return null;
  }

  @Nullable
  private static Object getAttribute(
          ResolvableMethodParameter parameter, WebSession session, Class<?> targetType, String key) {
    Object attribute = getAttribute(session, targetType, key);
    if (attribute != null) {
      return attribute;
    }
    else if (parameter.isNotRequired()) {
      return null;
    }

    throw new UnauthorizedException();
  }

}
