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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.blog.BlogApplication;
import cn.taketoday.blog.model.ArticleItem;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.SessionConfig;
import cn.taketoday.blog.web.console.ArticleForm;
import infra.app.test.context.InfraTest;
import infra.context.ApplicationContext;
import infra.context.annotation.Import;
import infra.core.ParameterizedTypeReference;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverters;
import infra.session.HeaderSessionIdResolver;
import infra.test.context.ActiveProfiles;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.test.web.mock.client.RestTestClient;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.transaction.annotation.Transactional;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/12 17:19
 */
@ActiveProfiles("test")
@Import(SessionConfig.class)
@InfraTest(classes = BlogApplication.class)
class ArticleHttpHandlerTests {

  MockMvcTester mvc;

  RestTestClient client;

  @BeforeEach
  void setup(ApplicationContext context) {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(get("/")
                    .header(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO, "key"))
            .build();

    mvc = MockMvcTester.create(mockMvc)
            .withHttpMessageConverters(HttpMessageConverters.ClientBuilder::registerDefaults);
    client = RestTestClient.bindTo(mockMvc)
            .configureMessageConverters(HttpMessageConverters.ClientBuilder::registerDefaults)
            .build();
  }

  @Test
  void uri() {
    String uri = "2024-year-end-summary&g_f=2000000392";
    int idx = uri.indexOf("&");
    if (idx > -1) {
      uri = uri.substring(0, idx);
    }

    assertThat(uri).isEqualTo("2024-year-end-summary");
  }

  @Test
  @Transactional
  public void homeArticles() {
    ArticleForm form = new ArticleForm();
    form.uri = "page";
    form.title = "page";

    createArticle(form);

    mvc.get().uri("/api/articles")
            .queryParam("page", "1")
            .queryParam("size", "1")
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .convertTo(new ParameterizedTypeReference<Pagination<ArticleItem>>() { })
            .satisfies(pagination -> {
              assertThat(pagination.getCurrent()).isEqualTo(1);
              assertThat(pagination.getSize()).isEqualTo(1);
              assertThat(pagination.getData()).isNotEmpty().hasSize(1);
              assertThat(pagination.getData().get(0)).extracting("title").isEqualTo("page");
              assertThat(pagination.getData().get(0)).extracting("uri").isEqualTo("page");
            });
  }

  @Test
  @Transactional
  public void detail() {
    ArticleForm form = new ArticleForm();
    form.uri = "test";
    form.title = "test";

    createArticle(form);

    mvc.get().uri("/api/articles/{uri}", form.uri)
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .extractingPath("$.uri").isEqualTo(form.uri);
  }

  private void createArticle(ArticleForm form) {
    client.post().uri("/api/console/articles").contentType(MediaType.APPLICATION_JSON)
            .body(form)
            .exchange()
            .expectStatus()
            .isCreated();
  }

}