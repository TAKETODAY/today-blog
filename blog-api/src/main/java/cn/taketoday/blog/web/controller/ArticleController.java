/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.ArticleItem;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.form.SearchForm;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.web.ArticlePasswordException;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.ArticleFilterInterceptor;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PATCH;
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
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-21 17:33
 */
@CustomLog
@RestController
@RequiredArgsConstructor
@Interceptor(ArticleFilterInterceptor.class)
@RequestMapping("/api/articles")
class ArticleController {

  private final LabelService labelService;

  private final ArticleService articleService;

  /**
   * 获取主页文章列表
   *
   * <pre>
   * {
   *   "total": 4759,
   *   "current":1,
   *   "data": [{
   *
   *   }],
   *   "pages":476,
   *   "size":10,
   * }
   * </pre>
   */
  @GET
  public Pagination<ArticleItem> homeArticles(Pageable pageable) {
    return articleService.getHomeArticles(pageable);
  }

  /**
   * 搜索接口
   *
   * @param q 查询
   */
  @GET(params = "q")
  public Pagination<ArticleItem> search(@RequestParam String q, Pageable pageable) {
    return articleService.search(BlogUtils.stripAllXss(q), pageable);
  }

  /**
   * 根据分类获取文章
   *
   * @param category 分类
   * @param pageable 分页
   */
  @GET(params = "category")
  public Pagination<ArticleItem> categories(@RequestParam String category, Pageable pageable) {
    return articleService.getArticlesByCategory(category, pageable);
  }

  /**
   * 根据标签获取对应文章
   *
   * @param tag 文章标签
   * @param pageable 分页
   */
  @GET(params = "tag")
  public Pagination<ArticleItem> byTag(@RequestParam String tag, Pageable pageable) {
    return articleService.getArticlesByTag(tag, pageable);
  }

  /**
   * 更新 PV
   *
   * @param id 文章ID
   * @param author 博主
   */
  @PATCH("/{id}/pv") // PATCH /api/articles/1560163530909/pv Referer
  public void updatePageView(@PathVariable("id") Long id,
          @RequestHeader String Referer, @Nullable Blogger author) {
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
   * 获取受欢迎的文章 API
   */
  @GET(params = "most-popular")
  public List<ArticleItem> mostPopular(Pageable pageable) {
    return articleService.getMostPopularArticles(pageable);
  }

  /**
   * 获取文章详情
   * <pre>{@code
   * {
   *   "pv": 25,
   *   "labels": [],
   *   "content": "",
   *   "cover": null,
   *   "summary": "",
   *   "updateAt": "",
   *   "createAt": "",
   *   "markdown": null,
   *   "category": "未分类",
   *   "id": 1577549633740,
   *   "copyright": "本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。",
   *   "title": "动态代理底层原理（Java）",
   *   "uri": "the-principles-of-dynamic-proxy-java"
   * }
   * }</pre>
   *
   * @param key 文章的密码，如果有的话
   * @param uri 文章地址
   * @param loginInfo 自动注入登录信息
   * @return {@link Article}
   */
  @GET("/{uri}")
  public Article detail(@Nullable String key, @PathVariable String uri, LoginInfo loginInfo) {
    Article article = articleService.getByURI(uri);
    if (article == null) {
      try {
        article = articleService.getById(Integer.parseInt(uri));
      }
      catch (NumberFormatException ignored) { }

      if (article == null) {
        throw ErrorMessageException.failed("文章不存在", HttpStatus.NOT_FOUND);
      }
    }

    if (loginInfo.isBloggerLoggedIn()) {
      return article;
    }

    if (article.getStatus() != PostStatus.PUBLISHED) { // 放在控制器层控制
      throw ErrorMessageException.failed("文章不能访问");
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
    article.setPassword(null);
    return article;
  }

  /**
   * 创建文章 API
   */
  @POST
  @RequiresBlogger
  @ResponseStatus(HttpStatus.CREATED)
  @Logging(title = "创建文章", content = "标题: [#{#form.title}]")
  public void create(@RequestBody ArticleForm form) {
    Article article = ArticleForm.forArticle(form, labelService);

    if (StringUtils.isBlank(article.getUri())) {
      article.setUri(form.title);
    }

    if (log.isDebugEnabled()) {
      log.debug("创建新文章: [{}]", form.title);
    }

    articleService.saveArticle(article);
  }

  @PUT("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequiresBlogger
  @Logging(title = "更新文章", content = "更新文章: [#{#from.title}]")
  public void update(@PathVariable("id") Integer id, @RequestBody ArticleForm from) {
    Article article = ArticleForm.forArticle(from, labelService);
    article.setId(id);
    article.setUpdateAt(LocalDateTime.now());
    articleService.update(article);
  }

  @PUT("/{id}/status/{status}")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "更新文章状态", content = "更新文章：[#{#id}]状态为：[#{#status}]")
  public void status(@PathVariable Long id, @PathVariable PostStatus status) {
    articleService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "删除文章", content = "删除文章: [#{#id}]")
  public void delete(@PathVariable Long id) {
    articleService.deleteById(id);
  }

  @GET("/admin")
  @RequiresBlogger
  public Pagination<Article> adminArticles(/*@RequestBody*/ SearchForm from, Pageable pageable) {
    return articleService.search(from, pageable);
  }

  @Nullable
  private static Set<Label> getLabels(ArticleForm from, LabelService labelService) {
    if (CollectionUtils.isNotEmpty(from.labels)) {
      var labels = new LinkedHashSet<Label>();
      for (String label : from.labels) {
        Label byName = labelService.getByName(label);
        if (byName == null) {
          byName = new Label().setName(label);
          labelService.save(byName);
        }
        labels.add(byName);
      }
      return labels;
    }
    return null;
  }

  static class ArticleForm {

    @Nullable
    public LocalDateTime createAt;

    public String category;
    public String copyright;
    public Set<String> labels;

    public String cover;
    public String title;
    public PostStatus status;
    public String summary;
    public String content;
    public String markdown;
    public String password;

    public String uri;

    static Article forArticle(ArticleForm form, LabelService labelService) {
      Set<Label> labels = getLabels(form, labelService);

      Article article = new Article();

      article.setLabels(labels);
      article.setTitle(form.title);
      article.setStatus(form.status);
      article.setContent(form.content);
      article.setSummary(form.summary);
      article.setCategory(form.category);
      article.setMarkdown(form.markdown);
      article.setCopyright(form.copyright);
      article.setPassword(StringUtils.hasText(form.password) ? form.password : null);
      article.setCover(StringUtils.hasText(form.cover)
                       ? form.cover
                       : BlogUtils.getFirstImagePath(form.content)
      );

      article.setUri(form.uri);
      article.setCreateAt(form.createAt);
      return article;
    }

  }
}
