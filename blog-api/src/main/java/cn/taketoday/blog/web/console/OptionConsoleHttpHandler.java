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

import org.jspecify.annotations.Nullable;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Option;
import cn.taketoday.blog.service.OptionService;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.web.annotation.GET;
import infra.web.annotation.PUT;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

/**
 * 博客系统选项控制台处理器。
 * <p>
 * 提供对系统选项（配置项）的增删改查接口，仅限博主访问。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2025/1/11 22:09
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/options")
class OptionConsoleHttpHandler {

  private final OptionService optionsService;

  public OptionConsoleHttpHandler(OptionService optionsService) {
    this.optionsService = optionsService;
  }

  /**
   * 获取所有系统选项列表。
   *
   * @return 系统选项列表
   */
  @GET
  public Pagination<Option> listOptions(@Nullable String name, Pageable pageable) {
    return optionsService.queryOptions(name, pageable);
  }

  @PUT
  @Logging(title = "更新系统变量")
  public void update(@RequestBody Option option) {
    optionsService.update(option);
  }

}
