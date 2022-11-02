/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.blog.ApplicationException;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.aspect.Logger;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.form.SearchForm;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.utils.BlogUtils;
import cn.taketoday.blog.utils.Pagination;
import cn.taketoday.blog.utils.StringUtils;
import cn.taketoday.blog.web.interceptor.BloggerInterceptor;
import cn.taketoday.blog.web.interceptor.ArticleFilterInterceptor;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.NotFoundException;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import lombok.CustomLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-21 17:33
 */
@CustomLog
@RestController
@RequiredArgsConstructor
@Interceptor(ArticleFilterInterceptor.class)
@RequestMapping("/api/articles")
public class ArticleController {

  private final LabelService labelService;
  private final ArticleService articleService;

  @Autowired
  CacheManager cacheManager;
//  @OPTIONS(value = "/**", combine = false)
//  public void options(RequestContext context) {
//
//    HttpHeaders requestHeaders = context.requestHeaders();
//
//    HttpHeaders responseHeaders = context.responseHeaders();
//    responseHeaders.setAccessControlMaxAge(3600);
//    responseHeaders.setAccessControlAllowCredentials(true);
//    responseHeaders.setAccessControlAllowOrigin(requestHeaders.getOrigin());
//
//    responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
//            "GET,POST,PUT,DELETE,PATCH,TRACE,HEAD,OPTIONS");
//    responseHeaders.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
//            "Authorization,Accept,X-Requested-With,Content-Type");
//  }

  /**
   * 获取主页文章列表
   *
   * <pre>
   * {
   *   "all": 4759,
   *   "current":1,
   *   "data": [{
   *
   *   }],
   *   "num":476,
   *   "size":10,
   * }
   * </pre>
   */
  @GET
  public Pagination<Article> articles(Pageable pageable) {
    int rowCount = articleService.countByStatus(PostStatus.PUBLISHED);
    assertFound(pageable, rowCount);
    List<Article> articles = articleService.getHomeArticles(pageable);
    return Pagination.ok(articles, rowCount, pageable);
  }

  static class HomeArticleReturnValue {
    public long id;
    public long pv;
    public String title;
    public String cover;
    public String summary;
    public List<String> tags;
  }

  /**
   * @param q query string
   */
  @GET("/search")
  public Pagination<Article> search(@RequestParam String q, Pageable pageable) {
    return articleService.search(BlogUtils.stripAllXss(q), pageable);
  }

  @GET("/categories/{category}")
  public Pagination<Article> categories(Pageable pageable, @PathVariable String category) {
    int rowCount = articleService.countByCategory(category);
    assertFound(pageable, rowCount);
    return Pagination.ok(articleService.getByCategory(category, pageable), rowCount, pageable);
  }

  protected void assertFound(Pageable pageable, int rowCount) {
    if (BlogUtils.notFound(pageable.getCurrent(), BlogUtils.pageCount(rowCount, pageable.getSize()))) {
      throw ApplicationException.failed("分页不存在");
    }
  }

  @GET("/tags/{label}")
  public Pagination<Article> tagJson(@PathVariable String label, Pageable pageable) {

    int rowCount = articleService.countByLabel(label);
    assertFound(pageable, rowCount);

    return Pagination.ok(articleService.getByLabel(label, pageable), rowCount, pageable);
  }

  @POST("/{id}/pv") // POST/blog-web/api/articles/1560163530909/pv Referer
  public void pv(@PathVariable("id") Long id, @RequestHeader String Referer, Blogger author) {
    if (author == null) {
      articleService.updatePageView(id);
    }
  }

  @GET("/{id}/tags")
  public Set<String> findArticleTags(@PathVariable Long id) {

    // log.debug("Get article id={}'s tags", id);
    return labelService.getByArticleId(id)//
            .stream()
            .map(Label::getName)
            .collect(Collectors.toSet());
  }

  /**
   * Get popular articles
   */
  @GET("/popular")
  public List<Article> popular() {
    return articleService.getMostPopularArticles();
  }

  /**
   * <pre>
   * {
   *   "pv": 25,
   *   "labels": [],
   *   "content": "",
   *   "image": null,
   *   "summary": "",
   *   "lastModify": 0,
   *   "markdown": null,
   *   "category": "未分类",
   *   "id": 1577549633740,
   *   "keepNavigation": false,
   *   "copyRight": "版权声明：本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。",
   *   "title": "中国学霸本科生提出AI新算法：速度比肩Adam，性能媲美SGD，ICLR领域主席赞不绝口",
   *   "url": "1577549633740"
   * }
   * </pre>
   *
   * @param key This article password if the article has
   * @param id article id
   * @param blogger if a {@link Blogger} login
   * @return {@link Article}
   */
  @GET("/{id}")
  public Article detail(@Nullable String key, @PathVariable("id") Long id, @Nullable Blogger blogger) {
    Article article = obtainById(id);
    if (blogger != null) {
      return article;
    }

    if (article.getStatus() != PostStatus.PUBLISHED) { // 放在控制器层控制
      throw new NotFoundException("文章不存在");
    }
    if (article.needPassword()) {
      if (key == null) {
        throw new ArticlePasswordException("需要访问密码");
      }
      if (!key.equals(article.getPassword())) {
        throw new ArticlePasswordException("访问密码错误");
      }
    }
    // 重置
    article.resetPassword();
    return article;
  }

  private Article obtainById(Long id) {
    Article article = articleService.getById(id);
    if (article == null) {
      throw new NotFoundException(id + " 文章不存在");
    }
    return article;
  }

  /**
   * Save article
   */
  @POST
  @ResponseStatus(HttpStatus.CREATED)
  @Interceptor(include = BloggerInterceptor.class, exclude = ArticleFilterInterceptor.class)
  @Logger(value = "创建文章", content = "创建新文章标题: [${from.title}]")
  public void create(@RequestBody ArticleFrom from) {
    Article article = getArticle(from);

    Long articleId = from.getCreateTime();
    if (articleId == null) {
      articleId = System.currentTimeMillis();
    }
    else {
      Article byId = articleService.getById(articleId);
      if (byId != null) {
        throw ApplicationException.failed("已经存在相同文章");
      }
    }
    article.setId(articleId);
    article.setLastModify(articleId);

    if (log.isDebugEnabled()) {
      log.debug("Create a new article: [{}]", from.getTitle());
    }

    articleService.saveArticle(article);
  }

  private Set<Label> getLabels(ArticleFrom from) {
    if (!CollectionUtils.isEmpty(from.getLabels())) {
      Set<Label> labels = new HashSet<>();
      for (String label : from.getLabels()) {
        Label byName = labelService.getByName(label);
        if (byName == null) {
          byName = new Label(System.currentTimeMillis()).setName(label);
          labelService.save(byName);
        }
        labels.add(byName);
      }
      return labels;
    }
    return null;
  }

  private Article getArticle(ArticleFrom from) {
    Set<Label> labels = getLabels(from);

    Article article = new Article();

    article.setLabels(labels);
    article.setTitle(from.getTitle());
    article.setStatus(from.getStatus());
    article.setContent(from.getContent());
    article.setSummary(from.getSummary());
    article.setCategory(from.getCategory());
    article.setMarkdown(from.getMarkdown());
    article.setCopyRight(from.getCopyRight());
    article.setPassword(StringUtils.isEmpty(from.getPassword()) ? null : from.getPassword());

    article.setImage(StringUtils.isEmpty(from.getImage())
                     ? BlogUtils.getFirstImagePath(from.getContent())
                     : from.getImage()
    );

    return article;
  }

  @Getter
  @Setter
  public static class ArticleFrom {

    private Long createTime;
    private String category;
    private String copyRight;
    private Set<String> labels;

    private String image;
    private String title;
    private PostStatus status;
    private String summary;
    private String content;
    private String markdown;
    private String password;
  }

  @PUT("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequiresBlogger
  @Logger(value = "更新文章", content = "更新文章: [${from.title}]")
  public void update(@PathVariable("id") Long id, @RequestBody ArticleFrom from) {
    Article article = getArticle(from);
    article.setId(id);
    articleService.update(article);
  }

  @PUT("/{id}/status/{status}")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logger(value = "更新文章状态", content = "更新文章：[${id}]状态为：[${status}]")
  public void status(@PathVariable Long id, @PathVariable PostStatus status) {
    articleService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logger(value = "删除文章", content = "删除文章: [${id}]")
  public void delete(@PathVariable Long id) {
    articleService.deleteById(id);
  }

  @GET("/admin")
  @RequiresBlogger
  public Pagination<Article> adminArticles(/*@RequestBody*/ SearchForm from, Pageable pageable) {
    return articleService.search(from, pageable);
  }

}
