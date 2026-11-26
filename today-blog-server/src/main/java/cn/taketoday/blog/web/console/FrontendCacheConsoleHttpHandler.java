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

import java.util.Map;

import cn.taketoday.blog.config.embedded.FrontendCacheSettings;
import cn.taketoday.blog.config.embedded.FrontendCacheSettings.View;
import cn.taketoday.blog.problem.CommonProblems;
import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.beans.factory.ObjectProvider;
import infra.http.CacheControl;
import infra.http.ResponseEntity;
import infra.web.annotation.GET;
import infra.web.annotation.PUT;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

/**
 * 管理后台的内嵌源站缓存设置接口。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
@RequiresBlogger
@RestController
@RequestMapping("/api/console/frontend-cache")
class FrontendCacheConsoleHttpHandler {

  private final ObjectProvider<FrontendCacheSettings> settings;

  FrontendCacheConsoleHttpHandler(ObjectProvider<FrontendCacheSettings> settings) {
    this.settings = settings;
  }

  @GET
  public ResponseEntity<View> get() {
    FrontendCacheSettings cache = settings.getIfAvailable();
    View view = cache == null ? new View(false, Map.of(), Map.of()) : cache.settings();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view);
  }

  @PUT
  public ResponseEntity<View> update(@RequestBody Map<String, String> presets) {
    FrontendCacheSettings cache = settings.getIfAvailable();
    if (cache == null) {
      throw ProblemException.of(CommonProblems.CONFLICT, "frontend.cache.embedded.required");
    }
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cache.update(presets));
  }

}
