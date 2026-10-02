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

import java.util.List;

import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.web.ErrorMessageException;
import infra.web.annotation.GET;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * 标签 HTTP 处理器，提供标签相关的 RESTful API 接口。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-30 11:05
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
class LabelHttpHandler {

  private final LabelService labelService;

  @GET
  public List<Label> get() {
    return labelService.getAllLabels();
  }

  @GET("/{name}")
  public Label name(@PathVariable String name) {
    Label label = labelService.getByName(name);
    ErrorMessageException.notNull(label, "标签不存在");
    return label;
  }

}
