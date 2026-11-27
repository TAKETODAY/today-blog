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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.env.MapPropertySource;
import infra.http.HttpHeaders;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.web.config.annotation.EnableWebMvc;

/**
 * 验证内嵌前端的 HTTP 路由和源站缓存策略。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
class FrontendAutoConfigurationTests {

  @Test
  void servesEachSpaAtItsMountPointAndOnRefresh() {
    try (var context = context(Map.of())) {
      var mvc = MockMvcTester.from(context);
      for (String path : new String[] { "/", "/articles/42" }) {
        mvc.get().uri(path).assertThat().hasStatusOk().hasBodyTextEqualTo("desktop\n");
      }
      for (String path : new String[] { "/blog-admin", "/blog-admin/", "/blog-admin/articles/edit" }) {
        mvc.get().uri(path).assertThat().hasStatusOk().hasBodyTextEqualTo("console\n");
      }
    }
  }

  @Test
  void doesNotFallBackForMissingAssetsOrReservedPaths() {
    try (var context = context(Map.of())) {
      var mvc = MockMvcTester.from(context);
      for (String path : new String[] { "/missing.js", "/blog-admin/missing.css", "/assets/missing.png",
              "/missing.js/", "/api", "/api/missing", "/sitemap.xml", "/feed.atom", "/feed.rss", "/upload", "/upload/missing" }) {
        mvc.get().uri(path).assertThat().hasStatus(404);
      }
      mvc.get().uri(URI.create("/missing%2Ejs")).assertThat().hasStatus(404);
    }
  }

  @Test
  void onlyContentHashedAssetsGetImmutableCacheByDefault() {
    try (var context = context(Map.of())) {
      var mvc = MockMvcTester.from(context);
      assertCache(mvc, "/articles/42", "no-cache");
      assertCache(mvc, "/blog-admin/articles/edit", "no-cache");
      assertCache(mvc, "/blog-admin/test.1234abcd.async.js", "public, max-age=31536000, immutable");
      assertCache(mvc, "/assets/test-DerJ7jKC.js", "public, max-age=31536000, immutable");
      assertCache(mvc, "/blog-admin/test-logo.svg", "public, max-age=86400");
      assertCache(mvc, "/test-overview.svg", "public, max-age=86400");
    }
  }

  @Test
  void consoleCacheOverridesDoNotChangeDesktopCache() {
    try (var context = context(Map.of(
            "frontend.cache.console.index", "no-store",
            "frontend.cache.console.assets", "private, max-age=60",
            "frontend.cache.console.hashed-assets", "public, max-age=3600"))) {
      var mvc = MockMvcTester.from(context);
      assertCache(mvc, "/blog-admin/articles/edit", "no-store");
      assertCache(mvc, "/blog-admin/test-logo.svg", "private, max-age=60");
      assertCache(mvc, "/blog-admin/test.1234abcd.async.js", "public, max-age=3600");
      assertCache(mvc, "/articles/42", "no-cache");
      assertCache(mvc, "/assets/test-DerJ7jKC.js", "public, max-age=31536000, immutable");
    }
  }

  @Test
  void usesUpdatedCachePoliciesWithoutRefreshingTheContext() {
    try (var context = context(Map.of())) {
      var mvc = MockMvcTester.from(context);
      assertCache(mvc, "/blog-admin/articles/edit", "no-cache");
      assertCache(mvc, "/assets/test-DerJ7jKC.js", "public, max-age=31536000, immutable");

      context.getEnvironment().getPropertySources().replace("frontend-test", new MapPropertySource("frontend-test", Map.of(
              "frontend.cache.console.index", "no-store",
              "frontend.cache.desktop.hashed-assets", "public, max-age=120")));

      assertCache(mvc, "/blog-admin/articles/edit", "no-store");
      assertCache(mvc, "/assets/test-DerJ7jKC.js", "public, max-age=120");
      assertCache(mvc, "/blog-admin/test.1234abcd.async.js", "public, max-age=31536000, immutable");
      assertCache(mvc, "/articles/42", "no-cache");
    }
  }

  private static void assertCache(MockMvcTester mvc, String path, String cacheControl) {
    mvc.get().uri(path).assertThat().hasStatusOk().hasHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
  }

  private static AnnotationConfigApplicationContext context(Map<String, Object> properties) {
    var context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("frontend-test", properties));
    context.register(WebConfiguration.class, FrontendAutoConfiguration.class);
    context.refresh();
    return context;
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class WebConfiguration {
  }

}
