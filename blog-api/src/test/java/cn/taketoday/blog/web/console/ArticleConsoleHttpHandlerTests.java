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

import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.web.WebAPITest;
import infra.beans.factory.annotation.Autowired;
import infra.http.MediaType;
import infra.persistence.EntityManager;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.test.web.mock.client.RestTestClient;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2026/3/7 22:04
 */
@WebAPITest
class ArticleConsoleHttpHandlerTests {

  @Autowired
  private MockMvcTester mvc;

  @Autowired
  private EntityManager entityManager;

  @Test
  @Transactional
  public void createAndUpdate(@Autowired RestTestClient client) {
    ArticleForm form = new ArticleForm();
    form.uri = "test";
    form.title = "test";

    // 创建文章
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
              article.setContent("updated content");

              // 更新文章
              client.put().uri("/api/console/articles/{id}", article.getId())
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(article)
                      .exchange()
                      .expectStatus().isNoContent();

              // 验证更新结果
              client.get().uri("/api/articles/{uri}", form.uri)
                      .accept(MediaType.APPLICATION_JSON)
                      .exchange()
                      .expectStatus().isOk()
                      .expectBody()
                      .jsonPath("$.id").isEqualTo(article.getId())
                      .jsonPath("$.uri").isEqualTo(article.getUri())
                      .jsonPath("$.content").isEqualTo(article.getContent());
            });
  }

  @Test
  @Transactional
  public void createAndDelete(@Autowired RestTestClient client) {
    ArticleForm form = new ArticleForm();
    form.uri = "delete-test";
    form.title = "delete test";

    // 创建文章
    client.post().uri("/api/console/articles").contentType(MediaType.APPLICATION_JSON)
            .body(form)
            .exchange()
            .expectStatus()
            .isCreated();

    // 获取文章 ID
    mvc.get().uri("/api/articles/{uri}", form.uri)
            .assertThat()
            .hasStatusOk()
            .bodyJson()
            .convertTo(Article.class)
            .satisfies(article -> {
              // 删除文章
              client.delete().uri("/api/console/articles/{id}", article.getId())
                      .exchange()
                      .expectStatus().isNoContent();

              // 验证已删除 — 公开接口应返回 404
              client.get().uri("/api/articles/{uri}", form.uri)
                      .exchange()
                      .expectStatus().isNotFound();
            });
  }

  @Test
  @Transactional
  public void updateStatus(@Autowired RestTestClient client) {
    ArticleForm form = new ArticleForm();
    form.uri = "status-test";
    form.title = "status test";

    createArticle(client, form);

    mvc.get().uri("/api/articles/{uri}", form.uri)
            .assertThat()
            .hasStatusOk()
            .bodyJson()
            .convertTo(Article.class)
            .satisfies(article -> {
              // 更新为草稿状态（使用枚举名 DRAFT）
              mvc.put().uri("/api/console/articles/{id}", article.getId())
                      .queryParam("status", "DRAFT")
                      .assertThat().hasStatusOk();

              // 验证状态已变更（博主可看任意状态的文章）
              client.get().uri("/api/articles/{uri}", form.uri)
                      .exchange()
                      .expectStatus().isOk()
                      .expectBody()
                      .jsonPath("$.status").isEqualTo("DRAFT");
            });
  }

  @Test
  @Transactional
  public void articlesList(@Autowired RestTestClient client) {
    // 创建两篇测试文章
    ArticleForm form1 = new ArticleForm();
    form1.uri = "list-test-1";
    form1.title = "list article 1";
    createArticle(client, form1);

    ArticleForm form2 = new ArticleForm();
    form2.uri = "list-test-2";
    form2.title = "list article 2";
    createArticle(client, form2);

    // 查询文章列表
    mvc.get().uri("/api/console/articles")
            .queryParam("page", "1")
            .queryParam("size", "10")
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .convertTo(new infra.core.ParameterizedTypeReference<cn.taketoday.blog.web.Pagination<Article>>() { })
            .satisfies(pagination -> {
              assertThat(pagination.getData()).isNotEmpty();
              assertThat(pagination.getData().size()).isGreaterThanOrEqualTo(2);
            });
  }

  private void createArticle(RestTestClient client, ArticleForm form) {
    client.post().uri("/api/console/articles").contentType(MediaType.APPLICATION_JSON)
            .body(form)
            .exchange()
            .expectStatus()
            .isCreated();
  }

}