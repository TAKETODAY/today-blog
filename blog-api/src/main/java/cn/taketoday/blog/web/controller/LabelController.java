/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.Json;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.web.BadRequestException;
import cn.taketoday.web.NotFoundException;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-30 11:05
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class LabelController {

  private final LabelService labelService;

  @POST("/{name}")
  @RequiresBlogger
  @Logging(title = "保存标签", content = "name: [${name}]")
  public void post(@PathVariable String name) {
    Label byName = labelService.getByName(name);
    if (byName != null) {
      throw ErrorMessageException.failed("标签重复");
    }
    labelService.save(new Label().setName(name));
  }

  @POST
  @RequiresBlogger
  @Logging(title = "批量保存标签", content = "names: ${Arrays.toString(#name)}")
  public Json post(@RequestParam(required = true) String[] name) {
    Set<Label> labels = new HashSet<>(name.length);
    for (String label : name) {
      if (StringUtils.isEmpty(label)) {
        throw new BadRequestException("标签名不能为空");
      }
      Label byName = labelService.getByName(label);
      if (byName != null) {
        return Json.failed("标签重复");
      }
      labels.add(new Label().setName(label));
    }

    labelService.saveAll(labels);
    return Json.ok("创建成功");
  }

  @PUT("/{id}")
  @RequiresBlogger
  @Logging(title = "标签更新", content = "update:[${#id}] with name:[${#name}]")
  public void put(@RequestParam(required = true) String name, @PathVariable int id) {
    Label label = labelService.getById(id);
    NotFoundException.notNull(label, "标签不存在");

    if (Objects.equals(label.getName(), name)) {
      throw ErrorMessageException.failed("标签名未更改");
    }

    labelService.update(label.setName(name));
  }

  @DELETE("/{id}")
  @RequiresBlogger
  @Logging(title = "删除标签", content = "delete id:[${#id}]")
  public void delete(@PathVariable long id) {
    Label byName = labelService.getById(id);
    NotFoundException.notNull(byName, "标签不存在");
    labelService.deleteById(byName.getId());
  }

  @GET
  public List<Label> get() {
    return labelService.getAllLabels();
  }

  @GET("/{name}")
  public Label name(@PathVariable String name) {
    return labelService.getByName(name);
  }

}
