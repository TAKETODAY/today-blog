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

import cn.taketoday.blog.BlogApplication;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.web.SessionConfig;
import infra.app.test.context.InfraTest;
import infra.beans.BeansException;
import infra.context.ApplicationContext;
import infra.context.annotation.Import;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverters;
import infra.persistence.EntityManager;
import infra.session.HeaderSessionIdResolver;
import infra.test.context.ActiveProfiles;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.test.web.mock.client.RestTestClient;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.transaction.annotation.Transactional;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2026/3/7 22:04
 */
@ActiveProfiles("test")
@Import({ SessionConfig.class })
@InfraTest(classes = BlogApplication.class)
class ArticleConsoleHttpHandlerTests {

  private final MockMvcTester mvc;

  private final RestTestClient client;

  private final EntityManager entityManager;

  ArticleConsoleHttpHandlerTests(ApplicationContext context) throws BeansException {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(get("/")
                    .header(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO, "key"))
            .build();

    mvc = MockMvcTester.create(mockMvc).withHttpMessageConverters(HttpMessageConverters.ClientBuilder::registerDefaults);
    client = RestTestClient.bindTo(mockMvc)
            .configureMessageConverters(HttpMessageConverters.ClientBuilder::registerDefaults)
            .build();

    this.entityManager = context.getBean(EntityManager.class);
  }

  @Test
  @Transactional
  public void update() {
    ArticleForm form = new ArticleForm();
    form.uri = "test";
    form.title = "test";

    client.post().uri("/api/console/articles").contentType(MediaType.APPLICATION_JSON)
            .body(form)
            .exchange()
            .expectStatus()
            .isCreated();

    mvc.get().uri("/api/articles/{uri}", form.uri).accept(MediaType.APPLICATION_JSON)
            .assertThat()
            .hasStatusOk()
            .bodyJson()
            .convertTo(Article.class)
            .satisfies(article -> {
              article.setContent("test");

              client.put().uri("/api/console/articles/{id}", article.getId())
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(article)
                      .exchange()
                      .expectStatus().isNoContent()
              ;

              client.get().uri("/api/articles/{uri}", form.uri)
                      .accept(MediaType.APPLICATION_JSON)
                      .exchange()
                      .expectStatus().isOk()
                      .expectBody()
                      .jsonPath("$.id").isEqualTo(article.getId())
                      .jsonPath("$.uri").isEqualTo(article.getUri())
                      .jsonPath("$.content").isEqualTo(article.getContent())
              ;

            });
  }

}