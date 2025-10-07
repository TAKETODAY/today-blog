/*
 * Copyright 2017 - 2025 the original author or authors.
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

package cn.taketoday.blog.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.blog.BlogApplication;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.console.ArticleForm;
import infra.app.test.context.InfraTest;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.http.MediaType;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.session.HeaderSessionIdResolver;
import infra.session.InMemorySessionRepository;
import infra.session.SessionEventDispatcher;
import infra.session.SessionIdGenerator;
import infra.session.SessionIdResolver;
import infra.session.SessionRepository;
import infra.session.Session;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.test.web.reactive.server.WebTestClient;
import infra.transaction.annotation.Isolation;
import infra.transaction.annotation.Transactional;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.request.MockMvcRequestBuilders.put;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/12 17:19
 */
@Import(ArticleControllerTests.SessionConfig.class)
@InfraTest(classes = BlogApplication.class)
class ArticleControllerTests {
  static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

  MockMvc mockMvc;

  @BeforeEach
  void setup(ApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(
                    get("/")
                            .header(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO, "key")
            )
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
  public void detail() throws Exception {
    ArticleForm form = new ArticleForm();
    form.uri = "test";

    mockMvc.perform(post("/api/articles")
                    .content("{\"uri\":\"test\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/api/articles/{uri}", form.uri))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.uri", equalTo(form.uri)));

  }

  @Test
  @Transactional(isolation = Isolation.READ_UNCOMMITTED)
  public void update() throws Exception {
    ArticleForm form = new ArticleForm();
    form.uri = "test";
    mockMvc.perform(post("/api/articles")
                    .content("{\"uri\":\"test\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

    WebTestClient client = MockMvcWebTestClient.bindTo(mockMvc).build();

    Article article = client.get()
            .uri("/api/articles/{uri}", form.uri)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .returnResult(Article.class)
            .getResponseBody().blockLast();

    assertThat(article).isNotNull();
    article.setContent("test");

    mockMvc.perform(put("/api/articles/{id}", article.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(article)))
            .andExpect(status().isNoContent());

    article = client.get()
            .uri("/api/articles/{uri}", form.uri)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .returnResult(Article.class)
            .getResponseBody().blockLast();

    assertThat(article).isNotNull();
    assertThat(article.getContent()).isEqualTo("test");
  }

  @Configuration
  static class SessionConfig {

    @Bean
    static SessionIdResolver sessionIdResolver() {
      return SessionIdResolver.authenticationInfo();
    }

    @Bean
    public SessionRepository sessionRepository(SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator) {
      return new InMemorySessionRepository(eventDispatcher, idGenerator) {
        @Override
        public Session createSession(String id) {
          Session session = super.createSession(id);
          new Blogger().bindTo(session);
          new User().bindTo(session);
          return session;
        }

        @Override
        public Session retrieveSession(String id) {
          Session session = super.retrieveSession(id);
          if (session == null) {
            session = createSession(id);
            session.start();
            session.save();
          }
          return session;
        }
      };
    }

  }

}