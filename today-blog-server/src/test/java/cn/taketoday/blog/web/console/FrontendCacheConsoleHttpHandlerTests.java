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

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.blog.config.embedded.FrontendCacheSettings;
import cn.taketoday.blog.problem.ProblemException;
import cn.taketoday.blog.util.ObjectUtils;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.ObjectProvider;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.jdbc.core.JdbcOperations;
import infra.test.web.mock.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/26
 */
@WebAPITest
class FrontendCacheConsoleHttpHandlerTests {

  private static final String URL = "/api/console/frontend-cache";

  @Autowired
  MockMvcTester mvc;

  @Autowired
  JdbcOperations jdbc;

  @Test
  void savesPresetsAndImmediatelyUsesThemForStaticResponses() throws Exception {
    try {
      update("{}").assertThat().hasStatusOk();
      mvc.get().uri(URL).assertThat().hasStatusOk().hasHeader(HttpHeaders.CACHE_CONTROL, "no-store")
              .bodyJson().extractingPath("$.effective['console.index']").isEqualTo("no-cache");

      update("""
              {"console.index":"no-store", "desktop.index":"5m", "desktop.hashed-assets":"1y", "upload":"1h"}
              """).assertThat().hasStatusOk().bodyJson()
              .extractingPath("$.effective['desktop.hashed-assets']").isEqualTo("public, max-age=31536000, immutable");
      var persisted = ObjectUtils.toMap(jdbc.queryForObject("select policies from t_frontend_cache where id = 1", String.class));
      assertThat(persisted).hasSize(4).containsEntry("console.index", "no-store").containsEntry("upload", "1h");

      update("""
              {"console.index":"1h", "upload":"public, max-age=123"}
              """).assertThat().hasStatus(400);
      update("""
              {"api":"no-store"}
              """).assertThat().hasStatus(400);
      mvc.get().uri("/blog-admin/system/frontend-cache").assertThat().hasStatusOk()
              .hasHeader(HttpHeaders.CACHE_CONTROL, "no-store");
      mvc.get().uri("/articles/42").assertThat().hasStatusOk()
              .hasHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=300");
    }
    finally {
      update("{}").assertThat().hasStatusOk();
    }
  }

  @Test
  void reportsNginxModeAndRejectsUpdates() {
    ObjectProvider<FrontendCacheSettings> provider = mock();
    var handler = new FrontendCacheConsoleHttpHandler(provider);
    assertThat(handler.get().getBody().embedded()).isFalse();
    assertThatExceptionOfType(ProblemException.class).isThrownBy(() -> handler.update(Map.of()));
  }

  @Test
  void requiresBloggerAuthentication() {
    mvc.get().uri(URL).requestAttr(LoginInfo.class.getName(), new LoginInfo()).assertThat().hasStatus(401);
    update("{}").requestAttr(LoginInfo.class.getName(), new LoginInfo()).assertThat().hasStatus(401);
  }

  private MockMvcTester.MockMvcRequestBuilder update(String json) {
    return mvc.put().uri(URL).contentType(MediaType.APPLICATION_JSON).content(json);
  }

}
