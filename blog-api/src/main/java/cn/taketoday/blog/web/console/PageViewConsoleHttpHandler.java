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

package cn.taketoday.blog.web.console;

import java.util.List;

import cn.taketoday.blog.model.PageView;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.persistence.EntityManager;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * 页面访问统计控制台处理器
 * <p>
 * 提供用于管理页面浏览量（Page View）数据的 RESTful API 接口。
 * 主要功能包括查询页面浏览记录等，仅限博主权限访问。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.3 2026/4/14 21:34
 */
@RequiresBlogger
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/console/pv")
class PageViewConsoleHttpHandler {

  private final EntityManager entityManager;

  @GET
  public List<PageView> listPageViews() {
    return entityManager.find(PageView.class);
  }

}
