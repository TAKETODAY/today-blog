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

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessage;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-27 20:44
 */
public class RandCodeInterceptor extends SessionHandlerInterceptor {

  public RandCodeInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  private void prepare(final RequestContext context) {
    context.setStatus(HttpStatus.BAD_REQUEST);
  }

  @Override
  public Object intercept(RequestContext context, InterceptorChain chain) throws Throwable {
    final String randCode = context.getParameter(BlogConstant.RAND_CODE);
    if (StringUtils.isEmpty(randCode)) {
      prepare(context);
      return ErrorMessage.failed("请输入验证码");
    }
    final Object attribute = getAttribute(context, BlogConstant.RAND_CODE);
    if (attribute == null || !randCode.equalsIgnoreCase(attribute.toString())) {
      prepare(context);
      return ErrorMessage.failed("验证码错误");
    }
    return chain.proceed(context);
  }
}
