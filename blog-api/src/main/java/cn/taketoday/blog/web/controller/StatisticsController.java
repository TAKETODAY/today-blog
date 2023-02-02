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
public class StatisticsController {
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
    statistics.articles = articleService.getLatest();
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
