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

import cn.taketoday.blog.config.BlogLocaleResolver;
import cn.taketoday.blog.model.Option;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.persistence.EntityManager;
import infra.test.web.mock.assertj.MockMvcTester;
import org.junit.jupiter.api.Test;

import static cn.taketoday.blog.service.OptionService.SITE_LOCALE;
import static cn.taketoday.blog.service.OptionService.SITE_TIME_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/19
 */
@WebAPITest
class SiteDisplayOptionsTests {

  @Autowired
  MockMvcTester mvc;

  @Autowired
  EntityManager entityManager;

  @Test
  void savesAndPublishesSiteDefaultsWithoutOverwritingThemOnInvalidInput() {
    Option locale = entityManager.findById(Option.class, SITE_LOCALE);
    Option zone = entityManager.findById(Option.class, SITE_TIME_ZONE);
    try {
      update(SITE_LOCALE, "en-US", true).assertThat().hasStatusOk();
      update(SITE_TIME_ZONE, "America/New_York", true).assertThat().hasStatusOk();
      update(SITE_TIME_ZONE, "+08:00", true).assertThat().hasStatus(400)
              .hasHeader(HttpHeaders.CONTENT_LANGUAGE, "en");
      update(SITE_LOCALE, "fr-FR", true).assertThat().hasStatus(400);
      update(SITE_LOCALE, "zh-CN", false).assertThat().hasStatus(400);

      assertThat(entityManager.findById(Option.class, SITE_LOCALE).getValue()).isEqualTo("en-US");
      assertThat(entityManager.findById(Option.class, SITE_TIME_ZONE).getValue()).isEqualTo("America/New_York");
      mvc.get().uri("/api/options").assertThat().hasStatusOk().bodyJson()
              .extractingPath("$['site.timezone']").isEqualTo("America/New_York");

      assertLanguage("en", "site", "zh-CN");
      assertLanguage("zh-CN", "zh-CN", "en-US");
      assertLanguage("zh-CN", "invalid", "fr;q=1,en;q=0.1,zh-CN;q=0.9");
      assertLanguage("en", "invalid", "fr,zh;q=0");
    }
    finally {
      update(SITE_LOCALE, locale.getValue(), true).assertThat().hasStatusOk();
      update(SITE_TIME_ZONE, zone.getValue(), true).assertThat().hasStatusOk();
    }
  }

  private MockMvcTester.MockMvcRequestBuilder update(String name, String value, boolean open) {
    return mvc.put().uri("/api/console/options").contentType(MediaType.APPLICATION_JSON)
            .header(BlogLocaleResolver.LOCALE_HEADER, "site")
            .content("""
                    {"name":"%s", "value":"%s", "open":%s, "valueType":"string"}
                    """.formatted(name, value, open));
  }

  private void assertLanguage(String expected, String selected, String browser) {
    mvc.get().uri("/api/articles/nonexistent-site-display-options-test")
            .header(BlogLocaleResolver.LOCALE_HEADER, selected)
            .header(HttpHeaders.ACCEPT_LANGUAGE, browser)
            .assertThat().hasStatus(404).hasHeader(HttpHeaders.CONTENT_LANGUAGE, expected);
  }

}
