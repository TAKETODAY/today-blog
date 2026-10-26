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

import java.nio.file.Path;

import javax.sql.DataSource;

import cn.taketoday.blog.updater.UpgradeFiles;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/** 不返回配置或异常详情；反向代理必须阻止外部访问此路径。 */
@RestController
@RequiredArgsConstructor
class HealthHttpHandler {
  private final DataSource dataSource;

  @GET("/api/internal/health")
  public String health() throws Exception {
    try (var connection = dataSource.getConnection()) {
      if (!connection.isValid(2)) throw new IllegalStateException("数据库不可用");
    }
    String release = System.getenv("BLOG_RELEASE_PATH");
    return "UP:" + (release == null ? "0" : UpgradeFiles.read(Path.of(release).resolve("release.properties")).getProperty("build"));
  }
}
