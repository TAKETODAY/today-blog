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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.blog.ApplicationException;
import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.utils.StringUtils;
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
    Assert.notNull(blogConfig);
    Assert.notNull(sessionResolver);
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
    return new RequestContextPageable(context);
  }

  final class RequestContextPageable implements Pageable, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer size;
    private Integer current;
    private final RequestContext request;

    public RequestContextPageable(RequestContext request) {
      this.request = request;
    }

    @Override
    public int getCurrent() {
      if (current == null) {
        String parameter = request.getParameter(pageRequestParameterName);
        if (StringUtils.isEmpty(parameter)) {
          current = 1;
        }
        else if ((current = parseInt(parameter)) <= 0) {
          throw ApplicationException.failed("分页页数必须大于0");
        }
      }
      return current;
    }

    @Override
    public int getSize() {
      if (size == null) {
        int size;
        String parameter = request.getParameter(pageSizeRequestParameterName);
        if (StringUtils.isEmpty(parameter)) {
          size = blogConfig.getListSize();
        }
        else {
          size = parseInt(parameter);
          if (size <= 0) {
            throw ApplicationException.failed("每页大小必须大于0");
          }

          if (size > blogConfig.getMaxPageSize()) {
            // 针对非博主的进行限制
            Blogger loggedInBlogger = sessionResolver.getLoggedInBlogger(request);
            if (loggedInBlogger == null) {
              throw ApplicationException.failed("分页大小超出限制");
            }
          }
        }
        return this.size = size;
      }
      return size;
    }

    private Integer parseInt(String parameter) {
      try {
        return Integer.valueOf(parameter);
      }
      catch (NumberFormatException e) {
        throw ApplicationException.failed("分页参数错误");
      }
    }

    // Object
    // ----------------------

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o instanceof RequestContextPageable that) {
        return Objects.equals(size, that.size)
                && Objects.equals(current, that.current);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(size, current);
    }
  }

}
