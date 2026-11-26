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

package cn.taketoday.blog.config.embedded;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import cn.taketoday.blog.config.AttachmentConfig;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.jdbc.core.JdbcOperations;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerResponse;
import infra.web.util.UriUtils;

/**
 * 在内嵌模式下从附件目录提供公开的本地附件。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "cn.taketoday.blog.frontend.FrontendAutoConfiguration")
class EmbeddedUploadConfiguration {

  @Bean
  FrontendCacheSettings frontendCacheSettings(ConfigurableEnvironment environment, JdbcOperations jdbc) {
    var settings = new FrontendCacheSettings(environment, jdbc);
    environment.getPropertySources().addFirst(settings);
    return settings;
  }

  @Bean
  RouterFunction<ServerResponse> embeddedUploadRouter(AttachmentConfig attachments, Environment environment) {
    Path root = Path.of(attachments.getUploadDir());
    return RouterFunctions.resources(request -> {
      String path = request.requestPath().pathWithinApplication().value();
      return path.startsWith("/upload/") ? findUpload(attachments, root, path) : Optional.empty();
    }, (resource, headers) -> {
      headers.setCacheControl(environment.getProperty("frontend.cache.upload", HttpHeaders.NO_CACHE));
      // 用户上传的 HTML 或 SVG 不能以博客同源页面的权限执行。
      headers.set("Content-Security-Policy", "sandbox; default-src 'none'");
      headers.set("X-Content-Type-Options", "nosniff");
    });
  }

  private static Optional<Resource> findUpload(AttachmentConfig attachments, Path root, String path) {
    try {
      String location = UriUtils.decode(path, StandardCharsets.UTF_8);
      Path file = attachments.getLocalFile(location).toPath().toRealPath();
      if (file.startsWith(root.toRealPath()) && Files.isRegularFile(file) && Files.isReadable(file)) {
        return Optional.of(new FileSystemResource(file));
      }
    }
    catch (IllegalArgumentException | IOException ex) {
      // 缺失的文件和非法路径均不参与路由，也不回退到 SPA。
      return Optional.empty();
    }
    return Optional.empty();
  }

}
