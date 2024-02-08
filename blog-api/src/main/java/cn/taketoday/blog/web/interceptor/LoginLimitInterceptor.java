/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.web.ErrorMessage;
import cn.taketoday.blog.web.Json;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.WebSession;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/8/8 12:37
 */
public class LoginLimitInterceptor extends SessionHandlerInterceptor {
  private String loginAttributeName = "login-times";

  private int loginMaxTryTimes = 10;

  public LoginLimitInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public Object intercept(RequestContext context, InterceptorChain chain) throws Throwable {
    if (!Blogger.isPresent(context)) {
      WebSession session = getSession(context); // TODO 客户端 删除了session-key 就会出错
      Object attribute = session.getAttribute(loginAttributeName);
      int loginTimes = 0;
      if (attribute instanceof Integer) {
        loginTimes = (int) attribute;
        if (loginTimes >= loginMaxTryTimes) {
          return ErrorMessage.failed("超过最大登录次数,请稍后重试");
        }
      }

      Object result = chain.proceed(context);
      if (result instanceof Json json) {
        if (!json.isSuccess()) {
          // 登录 失败
          loginTimes++;
          session.setAttribute(loginAttributeName, loginTimes);
        }
        else {
          session.removeAttribute(loginAttributeName);
        }
      }
      return result;
    }
    else {
      return chain.proceed(context);
    }
  }

  public void setLoginAttributeName(String loginAttributeName) {
    this.loginAttributeName = loginAttributeName;
  }

  public String getLoginAttributeName() {
    return loginAttributeName;
  }

  public void setLoginMaxTryTimes(int loginMaxTryTimes) {
    this.loginMaxTryTimes = loginMaxTryTimes;
  }

  public int getLoginMaxTryTimes() {
    return loginMaxTryTimes;
  }
}
