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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
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
import cn.taketoday.blog.web.LoginInfo;
import cn.taketoday.blog.web.interceptor.ArticleFilterInterceptor;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.NotFoundException;
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
  public Pagination<ArticleItem> articles(Pageable pageable) {
    int rowCount = articleService.countByStatus(PostStatus.PUBLISHED);
    assertFound(pageable, rowCount);
    List<ArticleItem> articles = articleService.getHomeArticles(pageable);
    return Pagination.ok(articles, rowCount, pageable);
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

  @GET("/categories/{category}")
  public Pagination<Article> categories(Pageable pageable, @PathVariable String category) {
    int rowCount = articleService.countByCategory(category);
    assertFound(pageable, rowCount);
    return Pagination.ok(articleService.getByCategory(category, pageable), rowCount, pageable);
  }

  protected void assertFound(Pageable pageable, int rowCount) {
    if (BlogUtils.notFound(pageable.getCurrent(), BlogUtils.pageCount(rowCount, pageable.getSize()))) {
      throw ErrorMessageException.failed("分页不存在");
    }
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
  public List<ArticleItem> mostPopular() {
    return articleService.getMostPopularArticles();
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
   *   "lastModify": 0,
   *   "markdown": null,
   *   "category": "未分类",
   *   "id": 1577549633740,
   *   "keepNavigation": false,
   *   "copyright": "版权声明：本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。",
   *   "title": "中国学霸本科生提出AI新算法：速度比肩Adam，性能媲美SGD，ICLR领域主席赞不绝口",
   *   "uri": "1577549633740"
   * }
   * }</pre>
   *
   * @param key This article password if the article has
   * @param uri 文章定位
   * @return {@link Article}
   */
  @GET("/{uri}")
  public Article detail(@Nullable String key, @PathVariable("uri") String uri, LoginInfo loginInfo) {
    Article article = articleService.getByURI(uri);
    if (article == null) {
      throw new NotFoundException("地址为 '" + uri + "' 的文章不存在");
    }

    if (loginInfo.isBloggerLoggedIn()) {
      return article;
    }

    if (article.getStatus() != PostStatus.PUBLISHED) { // 放在控制器层控制
      throw new NotFoundException("文章不能访问");
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
  @Logging(title = "创建文章", content = "创建新文章标题: [${#from.title}]")
  public void create(@RequestBody ArticleFrom from) {
    Article article = getArticle(from);

    Long articleId = from.getCreateTime();
    if (articleId == null) {
      articleId = System.currentTimeMillis();
    }
    else {
      Article byId = articleService.getById(articleId);
      if (byId != null) {
        throw ErrorMessageException.failed("已经存在相同文章");
      }
    }
    article.setId(articleId);
    if (!StringUtils.hasText(article.getUri())) {
      article.setUri(String.valueOf(articleId));
    }
    if (log.isDebugEnabled()) {
      log.debug("Create a new article: [{}]", from.getTitle());
    }

    articleService.saveArticle(article);
  }

  @Nullable
  private Set<Label> getLabels(ArticleFrom from) {
    if (CollectionUtils.isNotEmpty(from.getLabels())) {
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
    article.setCopyright(from.getCopyright());
    article.setPassword(StringUtils.isEmpty(from.getPassword()) ? null : from.getPassword());

    article.setCover(StringUtils.isEmpty(from.getImage())
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
    private String copyright;
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
  @Logging(title = "更新文章", content = "更新文章: [${#from.title}]")
  public void update(@PathVariable("id") Long id, @RequestBody ArticleFrom from) {
    Article article = getArticle(from);
    article.setId(id);
    articleService.update(article);
  }

  @PUT("/{id}/status/{status}")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "更新文章状态", content = "更新文章：[${#id}]状态为：[${#status}]")
  public void status(@PathVariable Long id, @PathVariable PostStatus status) {
    articleService.updateStatusById(status, id);
  }

  @DELETE("/{id}")
  @RequiresBlogger
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "删除文章", content = "删除文章: [${#id}]")
  public void delete(@PathVariable Long id) {
    articleService.deleteById(id);
  }

  @GET("/admin")
  @RequiresBlogger
  public Pagination<Article> adminArticles(/*@RequestBody*/ SearchForm from, Pageable pageable) {
    return articleService.search(from, pageable);
  }

}
