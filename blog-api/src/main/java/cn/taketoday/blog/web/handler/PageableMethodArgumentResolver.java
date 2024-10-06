/*
 * Copyright 2017 - 2024 the original author or authors.
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

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-25 01:12
 */
@Singleton
public class PageableMethodArgumentResolver implements ParameterResolvingStrategy {

  private final BlogConfig blogConfig;

  private final UserSessionResolver sessionResolver;

  private String pageRequestParameterName = BlogConstant.PARAMETER_CURRENT;

  private String pageSizeRequestParameterName = BlogConstant.PARAMETER_SIZE;

  public PageableMethodArgumentResolver(BlogConfig blogConfig, UserSessionResolver sessionResolver) {
    Assert.notNull(blogConfig, "BlogConfig is required");
    Assert.notNull(sessionResolver, "UserSessionResolver is required");
    this.blogConfig = blogConfig;
    this.sessionResolver = sessionResolver;
  }

  public void setPageRequestParameterName(@Nullable String pageRequestParameterName) {
    this.pageRequestParameterName = pageRequestParameterName == null ? BlogConstant.PARAMETER_CURRENT : pageRequestParameterName;
  }

  public void setPageSizeRequestParameterName(@Nullable String pageSizeRequestParameterName) {
    this.pageSizeRequestParameterName = pageSizeRequestParameterName == null ? BlogConstant.PARAMETER_SIZE : pageSizeRequestParameterName;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.isAssignableTo(Pageable.class);
  }

  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter parameter) {
    return new LazyPageable(blogConfig, sessionResolver, context, pageRequestParameterName, pageSizeRequestParameterName);
  }

}
