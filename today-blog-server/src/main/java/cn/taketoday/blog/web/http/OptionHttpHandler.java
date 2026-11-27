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

package cn.taketoday.blog.web.http;

import java.util.Map;

import cn.taketoday.blog.service.OptionService;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

/**
 * 博客选项 HTTP 处理器。
 * <p>
 * 提供与博客系统配置选项相关的公开接口。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-04-11 10:57
 */
@RestController
@RequestMapping("/api/options")
class OptionHttpHandler {

  private final OptionService optionsService;

  public OptionHttpHandler(OptionService optionsService) {
    this.optionsService = optionsService;
  }

  /**
   * 获取公开的选项列表。
   *
   * @return 包含公开选项的键值对映射
   */
  @GET
  public Map<String, String> publicOptions() {
    return optionsService.publicOptions();
  }

}
