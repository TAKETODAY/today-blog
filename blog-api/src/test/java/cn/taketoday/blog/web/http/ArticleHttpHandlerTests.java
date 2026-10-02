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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.ArticleItem;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.WebAPITest;
import cn.taketoday.blog.web.console.ArticleForm;
import infra.beans.factory.annotation.Autowired;
import infra.core.ParameterizedTypeReference;
import infra.http.MediaType;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.test.web.mock.client.RestTestClient;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/12 17:19
 */
@WebAPITest
class ArticleHttpHandlerTests {

  @Autowired
  private MockMvcTester mvc;

  @Autowired
  private RestTestClient client;

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

  @Test
  @Transactional
  public void detailNotFound() {
    // 不存在的文章应该返回 404
    mvc.get().uri("/api/articles/{uri}", "non-existent-uri-xxx")
            .assertThat().hasStatus(infra.http.HttpStatus.NOT_FOUND);
  }

  @Test
  @Transactional
  public void search() {
    ArticleForm form = new ArticleForm();
    form.uri = "search-test";
    form.title = "search-keyword-xyz";

    createArticle(form);

    mvc.get().uri("/api/articles")
            .queryParam("q", "search-keyword-xyz")
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .convertTo(new ParameterizedTypeReference<Pagination<ArticleItem>>() { })
            .satisfies(pagination -> {
              assertThat(pagination.getData()).isNotEmpty();
              assertThat(pagination.getData().get(0)).extracting("title").isEqualTo("search-keyword-xyz");
            });
  }

  @Test
  @Transactional
  public void categories() {
    ArticleForm form = new ArticleForm();
    form.uri = "cat-test";
    form.title = "cat test title";
    form.category = "test-category-unique";

    createArticle(form);

    mvc.get().uri("/api/articles")
            .queryParam("category", "test-category-unique")
            .queryParam("page", "1")
            .queryParam("size", "10")
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .convertTo(new ParameterizedTypeReference<Pagination<ArticleItem>>() { })
            .satisfies(pagination -> {
              assertThat(pagination.getData()).isNotEmpty();
              assertThat(pagination.getData().get(0)).extracting("title").isEqualTo("cat test title");
            });
  }

  @Test
  @Transactional
  public void byTag() {
    ArticleForm form = new ArticleForm();
    form.uri = "tag-test";
    form.title = "tag test title";
    form.labels = Set.of("tag-test-label");

    createArticle(form);

    mvc.get().uri("/api/articles")
            .queryParam("tag", "tag-test-label")
            .queryParam("page", "1")
            .queryParam("size", "10")
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .convertTo(new ParameterizedTypeReference<Pagination<ArticleItem>>() { })
            .satisfies(pagination -> {
              assertThat(pagination.getData()).isNotEmpty();
              assertThat(pagination.getData().get(0)).extracting("title").isEqualTo("tag test title");
            });
  }

  @Test
  @Transactional
  public void updatePageView() {
    ArticleForm form = new ArticleForm();
    form.uri = "pv-test";
    form.title = "pv test title";

    createArticle(form);

    // 获取文章详情以拿到 ID，然后 PATCH 更新 PV
    mvc.get().uri("/api/articles/{uri}", form.uri)
            .assertThat()
            .hasStatusOk()
            .bodyJson()
            .convertTo(Article.class)
            .satisfies(article -> {
              client.patch().uri("/api/articles/{id}/pv", article.getId())
                      .header("Referer", "https://example.com")
                      .exchange()
                      .expectStatus().isOk();
            });
  }

  @Test
  public void updatePageViewNotFound() {
    // 更新不存在的文章 PV，请求本身应成功返回
    client.patch().uri("/api/articles/{id}/pv", 999999999L)
            .header("Referer", "https://example.com")
            .exchange()
            .expectStatus().isOk();
  }

  @Test
  @Transactional
  public void findArticleTags() {
    ArticleForm form = new ArticleForm();
    form.uri = "tags-test";
    form.title = "tags test title";
    form.labels = Set.of("java", "infra");

    createArticle(form);

    // 获取文章详情以拿到 ID，然后查询标签
    mvc.get().uri("/api/articles/{uri}", form.uri)
            .assertThat()
            .hasStatusOk()
            .bodyJson()
            .convertTo(Article.class)
            .satisfies(article -> {
              mvc.get().uri("/api/articles/{id}/tags", article.getId())
                      .assertThat().hasStatusOk()
                      .hasContentType(MediaType.APPLICATION_JSON)
                      .bodyJson()
                      .convertTo(new ParameterizedTypeReference<Set<String>>() { })
                      .satisfies(tags -> {
                        assertThat(tags).contains("java", "infra");
                      });
            });
  }

  @Test
  @Transactional
  public void mostPopular() {
    ArticleForm form1 = new ArticleForm();
    form1.uri = "popular-1";
    form1.title = "popular article 1";
    createArticle(form1);

    ArticleForm form2 = new ArticleForm();
    form2.uri = "popular-2";
    form2.title = "popular article 2";
    createArticle(form2);

    mvc.get().uri("/api/articles")
            .queryParam("most-popular", "")
            .queryParam("page", "1")
            .queryParam("size", "10")
            .assertThat().hasStatusOk()
            .hasContentType(MediaType.APPLICATION_JSON)
            .bodyJson()
            .convertTo(new ParameterizedTypeReference<List<ArticleItem>>() { })
            .satisfies(list -> {
              assertThat(list).isNotEmpty();
            });
  }

  private void createArticle(ArticleForm form) {
    client.post().uri("/api/console/articles").contentType(MediaType.APPLICATION_JSON)
            .body(form)
            .exchange()
            .expectStatus()
            .isCreated();
  }

}