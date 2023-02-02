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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.model.Operation;
import cn.taketoday.blog.service.LoggingService;
import cn.taketoday.blog.utils.CSVUtils;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.Produce;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseStatus;
import lombok.CustomLog;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-06 17:37
 */
@CustomLog
@Controller
@RequestMapping("/api/logging")
@RequiresBlogger
public class LoggingController {

  private final LoggingService loggerService;

  public LoggingController(final LoggingService loggerService) {
    this.loggerService = loggerService;
  }

  @GET
  public Pagination<Operation> get(final Pageable pageable) {
    return loggerService.pagination(pageable);
  }

  @DELETE
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAll(@RequestParam("ids") long[] ids) {
    if (ObjectUtils.isEmpty(ids)) {
      loggerService.deleteAll();
    }
    else {
      if (log.isDebugEnabled()) {
        log.debug("删除：[{}]", Arrays.toString(ids));
      }
      loggerService.deleteByIds(ids);
    }
  }

  @DELETE("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteById(@PathVariable long id) {
    loggerService.deleteById(id);
  }

  @GET("/export.csv")
  @Produce("text/csv; charset=UTF-8")
  public StringBuilder export(/*ModelAndView modelAndView*/) {
    final List<Operation> all = loggerService.getAll();

    List<Map<String, Object>> data = new ArrayList<>(all.size());

    for (Operation operation : all) {
      Map<String, Object> map = new HashMap<>();
      map.put("id", operation.getId());
      map.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(operation.getId()));
      map.put("title", operation.getTitle());
      map.put("user", operation.getUser());
      map.put("ip", operation.getIp());
      map.put("content", operation.getContent());
      map.put("result", operation.getResult());
      map.put("type", operation.getType());
      data.add(map);
    }

    return CSVUtils.formatCsvData(data, "id,date,title,user,ip,content,result,type",
            "id,date,title,user,ip,content,result,type");
  }

}
