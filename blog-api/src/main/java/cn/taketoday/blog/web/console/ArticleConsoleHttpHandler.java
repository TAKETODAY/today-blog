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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.console;

import java.time.LocalDateTime;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.form.ArticleConditionForm;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.PATCH;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestController;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * 后台接口
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/30 22:27
 */
@CustomLog
@RequiresBlogger
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/console/articles")
class ArticleConsoleHttpHandler {

  private final LabelService labelService;

  private final ArticleService articleService;

  /**
   * 创建文章 API
   */
  @POST
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

  /**
   * 更新 API
   */
  @PUT("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "更新文章", content = "更新文章: [#{#from.title}]")
  public void update(@PathVariable("id") Long id, @RequestBody ArticleForm from) {
    Article article = ArticleForm.forArticle(from, labelService);
    article.setId(id);
    article.setUpdateAt(LocalDateTime.now());
    articleService.update(article);
  }

  /**
   * 更新状态 API
   */
  @PATCH(path = "/{id}", params = "status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "更新文章状态", content = "更新文章：[#{#id}]状态为：[#{#status}]")
  public void updateStatus(@PathVariable Long id, PostStatus status) {
    articleService.updateStatusById(status, id);
  }

  /**
   * 删除 API
   */
  @DELETE("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Logging(title = "删除文章", content = "删除文章: [#{#id}]")
  public void delete(@PathVariable Long id) {
    articleService.deleteById(id);
  }

  /**
   * 查询 文章列表 API
   */
  @GET
  public Pagination<Article> articles(ArticleConditionForm from, Pageable pageable) {
    return articleService.search(from, pageable);
  }

}
