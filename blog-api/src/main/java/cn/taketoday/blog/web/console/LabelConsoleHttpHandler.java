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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-30 11:05
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/tags")
@RequiredArgsConstructor
class LabelConsoleHttpHandler {

  private final LabelService labelService;

  @GET
  public List<Label> listTags() {
    return labelService.getAllLabels();
  }

  @POST("/{name}")
  @Logging(title = "保存标签", content = "name: [#{#name}]")
  public void post(@PathVariable String name) {
    Label byName = labelService.getByName(name);
    if (byName != null) {
      throw ErrorMessageException.failed("标签重复");
    }
    labelService.persist(Label.forName(name));
  }

  @POST
  @Logging(title = "批量保存标签", content = "names: #{Arrays.toString(#name)}")
  public void post(@RequestParam String[] name) {
    Set<Label> labels = new HashSet<>(name.length);
    for (String label : name) {
      if (StringUtils.isEmpty(label)) {
        throw ErrorMessageException.failed("标签名不能为空");
      }
      Label byName = labelService.getByName(label);
      if (byName != null) {
        throw ErrorMessageException.failed("标签重复");
      }
      labels.add(Label.forName(label));
    }

    labelService.persist(labels);
  }

  @PUT("/{id}")
  @Logging(title = "标签更新", content = "update:[#{#id}] with name:[#{#name}]")
  public void put(@RequestParam String name, @PathVariable int id) {
    Label label = labelService.getById(id);
    ErrorMessageException.notNull(label, "标签不存在");

    if (Objects.equals(label.getName(), name)) {
      throw ErrorMessageException.failed("标签名未更改");
    }
    label.setName(name);
    labelService.updateById(label);
  }

  @DELETE("/{id}")
  @Logging(title = "删除标签", content = "delete id:[#{#id}]")
  public void delete(@PathVariable long id) {
    Label byName = labelService.getById(id);
    ErrorMessageException.notNull(byName, "标签不存在");
    labelService.deleteById(byName.getId());
  }

}
