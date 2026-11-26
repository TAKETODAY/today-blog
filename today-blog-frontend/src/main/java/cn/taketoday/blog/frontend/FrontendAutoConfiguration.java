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

package cn.taketoday.blog.frontend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;
import java.util.function.Function;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerRequest;
import infra.web.handler.function.ServerResponse;

/**
 * 内嵌前端资源和 React 单页应用回退路由。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
@Configuration(proxyBeanMethods = false)
public class FrontendAutoConfiguration {

  private static final String CONSOLE_PREFIX = "/blog-admin";

  private static final String API_PREFIX = "/api";

  private static final Resource CONSOLE = new ClassPathResource("frontend/console/");

  private static final Resource DESKTOP = new ClassPathResource("frontend/desktop/");

  private static final Pattern HASHED_ASSET = Pattern.compile(".*(?:[.-])[0-9a-f]{8,}(?:[.-]).*",
          Pattern.CASE_INSENSITIVE);

  @Bean
  RouterFunction<ServerResponse> frontendRouter() {
    Function<ServerRequest, Optional<Resource>> lookup = this::lookup;
    BiConsumer<Resource, HttpHeaders> headers = this::setCacheHeaders;
    return RouterFunctions.resources(lookup, headers);
  }

  private Optional<Resource> lookup(ServerRequest request) {
    String path = request.path();
    if (path.equals(API_PREFIX) || path.startsWith(API_PREFIX + "/")) {
      return Optional.empty();
    }
    if (path.equals(CONSOLE_PREFIX) || path.startsWith(CONSOLE_PREFIX + "/")) {
      return find(CONSOLE, path.substring(CONSOLE_PREFIX.length()));
    }
    if (path.equals("/sitemap.xml") || path.equals("/feed.rss") || path.equals("/feed.atom")) {
      return Optional.empty();
    }
    return find(DESKTOP, path);
  }

  private Optional<Resource> find(Resource root, String path) {
    String relativePath = path;
    if (relativePath.isEmpty() || relativePath.equals("/")) {
      relativePath = "index.html";
    }
    else {
      relativePath = relativePath.substring(1);
      if (relativePath.isEmpty()) {
        relativePath = "index.html";
      }
    }
    if (!safe(relativePath)) {
      return Optional.empty();
    }

    try {
      Resource resource = root.createRelative(relativePath);
      if (resource.exists() && resource.isReadable()) {
        return Optional.of(resource);
      }
      if (!hasExtension(relativePath)) {
        Resource index = root.createRelative("index.html");
        if (index.exists() && index.isReadable()) {
          return Optional.of(index);
        }
      }
    }
    catch (IOException ex) {
      throw new UncheckedIOException("无法解析前端资源", ex);
    }
    return Optional.empty();
  }

  private static boolean safe(String path) {
    return !path.startsWith("/") && !path.contains("\\") && !path.contains("..")
            && !path.contains("//");
  }

  private static boolean hasExtension(String path) {
    int slash = path.lastIndexOf('/');
    int dot = path.lastIndexOf('.');
    return dot > slash;
  }

  private void setCacheHeaders(Resource resource, HttpHeaders headers) {
    if ("index.html".equals(resource.getName())) {
      headers.setCacheControl(HttpHeaders.NO_CACHE);
    }
    else if (HASHED_ASSET.matcher(resource.getName()).matches()) {
      headers.setCacheControl("public, max-age=31536000, immutable");
    }
    else {
      headers.setCacheControl("public, max-age=86400");
    }
  }

}
