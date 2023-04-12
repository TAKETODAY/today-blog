/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.blog.BlogApplication;
import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.web.controller.ArticleController.ArticleForm;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.http.MediaType;
import cn.taketoday.session.HeaderSessionIdResolver;
import cn.taketoday.session.InMemorySessionRepository;
import cn.taketoday.session.SessionEventDispatcher;
import cn.taketoday.session.SessionIdGenerator;
import cn.taketoday.session.SessionIdResolver;
import cn.taketoday.session.SessionRepository;
import cn.taketoday.session.WebSession;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.post;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/12 17:19
 */
@Import(ArticleControllerTests.SessionConfig.class)
@Transactional
@InfraTest(classes = BlogApplication.class)
class ArticleControllerTests {

  MockMvc mockMvc;

  @BeforeEach
  void setup(@Autowired ArticleController articleController,
          WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .defaultRequest(get("/")
                    .header(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO, "key"))
            .build();
//    mockMvc = MockMvcBuilders.standaloneSetup(articleController)
//            .defaultRequest(get("/")
//                    .header(HeaderSessionIdResolver.HEADER_AUTHENTICATION_INFO, "key"))
//            .build();
  }

  @Test
  void detail() throws Exception {
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

  @Configuration
  static class SessionConfig {

    @Bean
    SessionIdResolver sessionIdResolver() {
      return HeaderSessionIdResolver.authenticationInfo();
    }

    @Bean
    SessionRepository sessionRepository(
            SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator) {
      return new InMemorySessionRepository(eventDispatcher, idGenerator) {
        @Override
        public WebSession createSession(String id) {
          WebSession session = super.createSession(id);
          session.setAttribute(BlogConstant.BLOGGER_INFO, new Blogger());
          session.setAttribute(BlogConstant.USER_INFO, new User());
          return session;
        }

        @Override
        public WebSession retrieveSession(String id) {
          WebSession session = super.retrieveSession(id);
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