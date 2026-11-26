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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.env.Environment;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerRequest;
import infra.web.handler.function.ServerResponse;
import infra.web.util.UriUtils;

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

  private static final ClassPathResource CONSOLE = new ClassPathResource("frontend/console/");

  private static final ClassPathResource DESKTOP = new ClassPathResource("frontend/desktop/");

  private static final Pattern CONSOLE_HASH = Pattern.compile(".+\\.[0-9a-fA-F]{8}\\..+");

  private static final Pattern DESKTOP_HASH = Pattern.compile(".+-[A-Za-z0-9_-]{8}\\..+");

  @Bean
  RouterFunction<ServerResponse> frontendRouter(Environment environment) {
    var consoleCache = CachePolicy.load(environment, "frontend.cache.console.");
    var desktopCache = CachePolicy.load(environment, "frontend.cache.desktop.");
    return RouterFunctions.resources(this::lookupConsole,
                    (resource, headers) -> consoleCache.apply(resource, headers,
                            asset -> CONSOLE_HASH.matcher(asset.getName()).matches()))
            .and(RouterFunctions.resources(this::lookupDesktop,
                    (resource, headers) -> desktopCache.apply(resource, headers,
                            asset -> asset.getPath().startsWith(DESKTOP.getPath() + "assets/")
                                    && DESKTOP_HASH.matcher(asset.getName()).matches())));
  }

  private Optional<Resource> lookupConsole(ServerRequest request) {
    String path = request.requestPath().pathWithinApplication().value();
    if (under(path, CONSOLE_PREFIX)) {
      return find(CONSOLE, path.substring(CONSOLE_PREFIX.length()));
    }
    return Optional.empty();
  }

  private Optional<Resource> lookupDesktop(ServerRequest request) {
    String path = request.requestPath().pathWithinApplication().value();
    if (under(path, API_PREFIX) || under(path, CONSOLE_PREFIX) || under(path, "/upload")) {
      return Optional.empty();
    }
    if (path.equals("/sitemap.xml") || path.equals("/feed.rss") || path.equals("/feed.atom")) {
      return Optional.empty();
    }
    return find(DESKTOP, path);
  }

  private static boolean under(String path, String prefix) {
    return path.equals(prefix) || path.startsWith(prefix + "/");
  }

  private Optional<Resource> find(Resource root, String path) {
    String relativePath;
    try {
      relativePath = path.length() <= 1 ? "index.html" : UriUtils.decode(path.substring(1), StandardCharsets.UTF_8);
    }
    catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
    if (!safe(relativePath)) {
      return Optional.empty();
    }

    try {
      Resource resource = root.createRelative(relativePath);
      if (resource.isReadable()) {
        return Optional.of(resource);
      }
      if (!hasExtension(relativePath)) {
        Resource index = root.createRelative("index.html");
        if (index.isReadable()) {
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
    // 尾部斜杠不应把缺失的文件请求变成 SPA 路由。
    String name = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    return name.lastIndexOf('.') > name.lastIndexOf('/');
  }

  private record CachePolicy(String index, String assets, String hashedAssets) {

    static CachePolicy load(Environment environment, String prefix) {
      return new CachePolicy(environment.getProperty(prefix + "index", HttpHeaders.NO_CACHE),
              environment.getProperty(prefix + "assets", "public, max-age=86400"),
              environment.getProperty(prefix + "hashed-assets", "public, max-age=31536000, immutable"));
    }

    void apply(Resource resource, HttpHeaders headers, Predicate<ClassPathResource> hashed) {
      String name = resource.getName();
      if ("index.html".equals(name)) {
        headers.setCacheControl(index);
      }
      else if (name != null && resource instanceof ClassPathResource asset && hashed.test(asset)) {
        headers.setCacheControl(hashedAssets);
      }
      else {
        headers.setCacheControl(assets);
      }
    }
  }

}
