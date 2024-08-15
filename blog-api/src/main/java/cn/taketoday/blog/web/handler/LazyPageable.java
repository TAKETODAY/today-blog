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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.config.UserSessionResolver;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2.0 2024/8/15 21:18
 */
final class LazyPageable implements Pageable, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private Integer size;

  @Nullable
  private Integer current;

  private final BlogConfig blogConfig;

  private final RequestContext request;

  private final UserSessionResolver sessionResolver;

  private final String pageRequestParameterName;

  private final String pageSizeRequestParameterName;

  public LazyPageable(BlogConfig blogConfig, UserSessionResolver sessionResolver,
          RequestContext request, String pageRequestParameterName, String pageSizeRequestParameterName) {
    this.blogConfig = blogConfig;
    this.sessionResolver = sessionResolver;
    this.request = request;
    this.pageRequestParameterName = pageRequestParameterName;
    this.pageSizeRequestParameterName = pageSizeRequestParameterName;
  }

  @Override
  public int pageNumber() {
    if (current == null) {
      String parameter = request.getParameter(pageRequestParameterName);
      if (StringUtils.isEmpty(parameter)) {
        current = 1;
      }
      else if ((current = parseInt(parameter)) <= 0) {
        throw ErrorMessageException.failed("分页页数必须大于0");
      }
    }
    return current;
  }

  @Override
  public int pageSize() {
    if (size == null) {
      int size;
      String parameter = request.getParameter(pageSizeRequestParameterName);
      if (StringUtils.isEmpty(parameter)) {
        size = blogConfig.listSize;
      }
      else {
        size = parseInt(parameter);
        if (size <= 0) {
          throw ErrorMessageException.failed("每页大小必须大于0");
        }

        if (size > blogConfig.maxPageSize) {
          // 针对非博主的进行限制
          Blogger loggedInBlogger = sessionResolver.getLoggedInBlogger(request);
          if (loggedInBlogger == null) {
            throw ErrorMessageException.failed("分页大小超出限制");
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
      throw ErrorMessageException.failed("分页参数错误");
    }
  }

  // Object
  // ----------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof LazyPageable that) {
      return Objects.equals(size, that.size)
              && Objects.equals(current, that.current);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(size, current);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("size", size)
            .append("current", current)
            .toString();
  }
}
