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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.Operation;
import cn.taketoday.blog.model.enums.StatisticsField;
import cn.taketoday.blog.model.form.PageViewStatistics;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.service.AttachmentService;
import cn.taketoday.blog.service.CommentService;
import cn.taketoday.blog.service.LoggingService;
import cn.taketoday.blog.service.StatisticsService;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-08-24 20:59
 */
@RestController
@RequiresBlogger
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
class StatisticsController {

  private final BlogConfig blogConfig;

  private final LoggingService loggerService;

  private final CommentService commentService;

  private final ArticleService articleService;

  private final AttachmentService attachmentService;

  private final StatisticsService statisticsService;

  @GET("/dashboard")
  public DashboardStatistics dashboard() {
    final DashboardStatistics statistics = new DashboardStatistics();
    statistics.articleCount = articleService.count();
    statistics.commentCount = commentService.count();
    statistics.attachmentCount = attachmentService.count();
    statistics.logs = loggerService.getLatest();
    statistics.articles = articleService.getLatestArticles();
    statistics.comments = commentService.getLatest();

    return statistics;
  }

  class DashboardStatistics {
    public final long lastStartup = blogConfig.getStartupTimeMillis();
    public int articleCount;
    public int commentCount;
    public int attachmentCount;

    public List<Operation> logs;
    public List<Article> articles;
    public List<Comment> comments;
  }

  //LocalDate from, LocalDate to

  @GET("/{type}")
  public Map<String, Integer> statistics(@PathVariable StatisticsField type) {
    return statisticsService.analyze(type);
  }

  @GET("/pv")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  public Map<String, PageViewStatistics> statistics(LocalDate from, LocalDate to) {
    return statisticsService.analyzePageView(from, to);
  }

}
