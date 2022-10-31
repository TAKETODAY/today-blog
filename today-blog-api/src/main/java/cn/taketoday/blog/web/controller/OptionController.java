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

package cn.taketoday.blog.web.controller;

import java.util.Map;

import cn.taketoday.blog.aspect.Logger;
import cn.taketoday.blog.service.OptionService;
import cn.taketoday.blog.web.interceptor.BloggerInterceptor;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-04-11 10:57
 */
@RestController
@RequestMapping("/api/options")
@RequiresBlogger
public class OptionController {

  private final OptionService optionsService;

  public OptionController(final OptionService optionsService) {
    this.optionsService = optionsService;
  }

  @GET
  @Interceptor(exclude = BloggerInterceptor.class)
  public Map<String, String> get() {
    return optionsService.getOptionsMap();
  }

  @PUT
  @Logger("更新系统变量")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void put(@RequestBody Map<String, String> options) {

    optionsService.update(options);
  }

}
