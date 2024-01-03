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

package cn.taketoday.blog.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.config.BlogConfig;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.ArticleItem;
import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.model.Sitemap;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.feed.Atom;
import cn.taketoday.blog.model.feed.Entry;
import cn.taketoday.blog.model.feed.Item;
import cn.taketoday.blog.model.feed.Rss;
import cn.taketoday.blog.model.form.SearchForm;
import cn.taketoday.blog.repository.ArticleRepository;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.jdbc.AbstractQuery;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.InternalServerException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@Service
@CustomLog
@RequiredArgsConstructor
@CacheConfig(cacheNames = "Articles")
public class ArticleService implements InitializingBean {
  private final BlogConfig blogConfig;
  private final LabelService labelService;
  private final RepositoryManager repository;
  private final CategoryService categoryService;
  private final ArticleRepository articleRepository;

  private final Rss rss = new Rss();
  private final Atom atom = new Atom();
  private final Sitemap sitemap = new Sitemap();

  /**
   * Update {@link Article}
   *
   * @param article article instance
   */
  @Transactional
  @CacheEvict(key = "'ById_'+#article.id")
  public void update(Article article) {
    Assert.notNull(article, "文章不能为空");
    Assert.notNull(article.getId(), "文章ID不能为空");
    Article oldArticle = obtainById(article.getId());

    repository.getEntityManager().updateById(article);

    // update category
    if (!Objects.equals(article.getCategory(), oldArticle.getCategory())) {
      try {
        categoryService.updateArticleCount(article.getCategory());
        categoryService.updateArticleCount(oldArticle.getCategory());
      }
      catch (Exception e) {
        throw InternalServerException.failed("文章分类更新失败", e);
      }
    }

    try {
      // 更新标签
      updateArticleLabels(article, oldArticle);
    }
    catch (Exception e) {
      throw InternalServerException.failed("文章标签更新失败", e);
    }

    try {
      refreshFeedArticles();
    }
    catch (Exception e) {
      throw InternalServerException.failed("文章订阅更新失败", e);
    }
  }

  protected void updateArticleLabels(Article newArticle, Article articleInDb) {
    Set<Label> oldLabels = articleInDb.getLabels();
    Set<Label> newLabels = newArticle.getLabels();
    if (labelsChanged(newLabels, oldLabels)) {
      if (CollectionUtils.isNotEmpty(oldLabels)) {
        labelService.removeArticleLabels(newArticle.getId());
      }
      if (CollectionUtils.isNotEmpty(newLabels)) {
        labelService.saveArticleLabels(newLabels, newArticle.getId());
      }
    }
  }

  /**
   * If {@link Label}s changed
   */
  protected boolean labelsChanged(Set<Label> newLabels, Set<Label> oldLabels) {
    if (CollectionUtils.isEmpty(newLabels)) {
      return CollectionUtils.isNotEmpty(oldLabels);
    }
    return !Objects.equals(newLabels, oldLabels);
  }

  /**
   * 更新PV
   */
  public void updatePageView(long id) {
    try (var query = repository.createQuery("update article set `pv`= pv + 1 where `id` = ?")) {
      query.addParameter(id);
      query.executeUpdate();
    }
  }

  @Cacheable(key = "'ById_'+#id")
  public Article getById(long id) {
    try (Query query = repository.createQuery("SELECT * FROM article WHERE id = ? LIMIT 1")) {
      query.addParameter(id);

      Article article = query.fetchFirst(Article.class);
      applyTags(article);
      return article;
    }
  }

  @Nullable
  @Cacheable(key = "'getByURI_'+#uri")
  public Article getByURI(String uri) {
    Assert.notNull(uri, "文章地址不能为空");
    try (Query query = repository.createQuery("SELECT * FROM article WHERE uri=? LIMIT 1")) {
      query.addParameter(uri);

      Article article = query.fetchFirst(Article.class);
      applyTags(article);
      return article;
    }
  }

  /**
   * @return {@link Article} never be null
   */
  protected Article obtainById(long id) {
    Article byId = getById(id);
    if (byId == null) {
      throw ErrorMessageException.failed("该文章不存在或已删除不能操作");
    }
    return byId;
  }

  // 获取文章列表

  /**
   * 获取受欢迎的文章
   * <p>
   * 根据点击量排序
   *
   * @return 受欢迎的文章
   */
  public List<ArticleItem> getMostPopularArticles(Pageable pageable) {
    try (var query = repository.createQuery("""
            SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
            FROM article WHERE status = ? order by pv DESC LIMIT ?""")) {
      query.addParameter(PostStatus.PUBLISHED);
      query.addParameter(pageable.size(20));
      return applyTags(query.fetch(ArticleItem.class));
    }
  }

  /**
   * 获取首页文章
   */
  @Cacheable(key = "'home-'+#pageable.current()+'-'+#pageable.size()")
  public Pagination<ArticleItem> getHomeArticles(Pageable pageable) {
    try (JdbcConnection connection = repository.open()) {
      try (Query countQuery = connection.createQuery(
              "SELECT COUNT(id) FROM article WHERE `status` = ?")) {
        countQuery.addParameter(PostStatus.PUBLISHED);
        int count = countQuery.fetchScalar(int.class);
        if (count < 1) {
          return Pagination.empty();
        }

        String sql = """
                SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
                FROM article WHERE `status` = :status
                order by create_at DESC LIMIT :offset, :pageSize
                """;
        try (NamedQuery query = repository.createNamedQuery(sql)) {
          query.addParameter("offset", pageable.offset());
          query.addParameter("status", PostStatus.PUBLISHED);
          query.addParameter("pageSize", pageable.size());

          return fetchArticleItems(pageable, count, query);
        }
      }
    }
  }

  private Pagination<ArticleItem> fetchArticleItems(Pageable pageable, int count, AbstractQuery query) {
    List<ArticleItem> items = applyTags(query.fetch(ArticleItem.class));
    return Pagination.ok(items, count, pageable);
  }

  /**
   * 搜索文章
   */
  public Pagination<ArticleItem> search(String q, Pageable pageable) {
    try (JdbcConnection connection = repository.open()) {
      try (NamedQuery countQuery = connection.createNamedQuery(
              "select count(*) from article WHERE `title` like :q OR `content` like :q")) {
        countQuery.addParameter("q", "%" + q + "%");
        int count = countQuery.fetchScalar(int.class);
        if (count < 1) {
          return Pagination.empty();
        }
        try (NamedQuery dataQuery = connection.createNamedQuery("""
                SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
                FROM article WHERE `title` LIKE :q OR `content` LIKE :q
                ORDER BY create_at DESC LIMIT :offset, :size""")) {

          dataQuery.addParameter("q", "%" + q + "%");
          dataQuery.addParameter("offset", pageable.offset());
          dataQuery.addParameter("size", pageable.size());
          return fetchArticleItems(pageable, count, dataQuery);
        }
      }
    }
  }

  public Pagination<Article> search(SearchForm from, Pageable pageable) {
//    try (Query query = repository.createQuery(
//            "SELECT COUNT(id) FROM article")) {
//      query.executeUpdate();
//    }

    int count = articleRepository.getRecord(from);
    if (count < 1) {
      return Pagination.empty();
    }
    List<Article> articles = articleRepository.find(from, pageable.offset(), pageable.size());
    return Pagination.ok(articles, count, pageable);
  }

  /**
   * 更具标签获取对应文章
   */
  public Pagination<ArticleItem> getArticlesByTag(String label, Pageable pageable) {
    try (JdbcConnection connection = repository.open()) {
      try (var countQuery = connection.createNamedQuery("""
              SELECT COUNT(*) FROM article
              LEFT JOIN article_label ON article.id = article_label.articleId
              WHERE status = :status
                and article_label.labelId IN (
                  SELECT labelId FROM article_label
                    WHERE labelId = (SELECT id FROM label WHERE name = :name))""")) {

        countQuery.addParameter("name", label);
        countQuery.addParameter("status", PostStatus.PUBLISHED);
        int count = countQuery.fetchScalar(int.class);
        if (count < 1) {
          return Pagination.empty();
        }
        try (var dataQuery = connection.createNamedQuery("""
                SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
                FROM article LEFT JOIN article_label ON article.id = article_label.articleId
                WHERE article.status = :status
                  AND article_label.labelId IN (
                    SELECT labelId FROM article_label
                      WHERE labelId = (SELECT id FROM label WHERE name = :name)
                  )
                LIMIT :offset, :size""")) {

          dataQuery.addParameter("name", label);
          dataQuery.addParameter("status", PostStatus.PUBLISHED);
          dataQuery.addParameter("offset", pageable.offset());
          dataQuery.addParameter("size", pageable.size());
          return fetchArticleItems(pageable, count, dataQuery);
        }
      }
    }
  }

  /***
   * 根据类型找文章
   * @param pageable 分页
   */
  @Cacheable(key = "'cate_'+#categoryName+'_'+#pageable")
  public Pagination<ArticleItem> getArticlesByCategory(String categoryName, Pageable pageable) {
    try (JdbcConnection connection = repository.open()) {

      try (var countQuery = connection.createNamedQuery("""
              SELECT COUNT(id) FROM article WHERE status = :status AND category = :name""")) {
        countQuery.addParameter("name", categoryName);
        countQuery.addParameter("status", PostStatus.PUBLISHED);
        int count = countQuery.fetchScalar(int.class);
        if (count < 1) {
          return Pagination.empty();
        }

        try (var dataQuery = connection.createNamedQuery("""
                SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
                FROM article WHERE status = :status AND category = :name LIMIT :offset, :pageSize""")) {
          dataQuery.addParameter("name", categoryName);
          dataQuery.addParameter("status", PostStatus.PUBLISHED);
          dataQuery.addParameter("offset", pageable.offset());
          dataQuery.addParameter("pageSize", pageable.size());
          return fetchArticleItems(pageable, count, dataQuery);
        }
      }
    }
  }

  /**
   * 刷新订阅文章
   */
  public void refreshFeedArticles() {
    int listSize = blogConfig.getArticleFeedListSize();
    try (Query query = repository.createQuery(
            "SELECT * FROM article WHERE status=0 order by id DESC LIMIT ?")) {
      query.addParameter(listSize);

      List<Article> feedArticles = query.fetch(Article.class);
      applyLabels(feedArticles);

      buildAtom(feedArticles);
      buildRss(feedArticles);
      buildSitemap();
    }
  }

  @Override
  public void afterPropertiesSet() {
    refreshFeedArticles();
  }

  /**
   * Atom
   */
  protected void buildAtom(List<Article> feedArticles) {
    log.debug("Build Atom ");
    atom.getEntries().clear();

    for (Article article : feedArticles) {
      if (article.needPassword()) {
        continue;
      }

      Entry entry = new Entry();
      entry.setId(article.getId());
      entry.setImage(article.getCover());
      entry.setTitle(article.getTitle());
      entry.setPublished(article.getCreateAt());
      entry.setSummary(article.getSummary());
      entry.setContent(article.getContent());
      entry.setUpdated(article.getUpdateAt());

      entry.addCategories(article.getLabels()
              .stream()
              .map(Label::getName)
              .collect(Collectors.toSet())
      );

      atom.addEntry(entry);
    }

    atom.setUpdated(System.currentTimeMillis());
  }

  /**
   * Rss
   */
  protected void buildRss(List<Article> feedArticles) {

    log.debug("Build Rss");
    rss.getItems().clear();

    for (Article article : feedArticles) {
      if (article.needPassword()) {
        continue;
      }

      Item item = new Item();
      item.setId(article.getId());
      item.setTitle(article.getTitle());
      item.setImage(article.getCover());
      item.setSummary(article.getSummary());
      item.setContent(article.getContent());
      item.setPubDate(article.getId());

      item.addCategories(article.getLabels()
              .stream()
              .map(Label::getName)
              .collect(Collectors.toSet())
      );
      rss.addItem(item);
    }

    rss.setLastBuildDate(System.currentTimeMillis());
  }

  protected synchronized void buildSitemap() {
    log.debug("Build Sitemap");
    try (var query = repository.createQuery("SELECT * FROM article ORDER BY create_at DESC")) {
      sitemap.getUrls().clear();
      for (Article article : query.fetch(Article.class)) {
        if (article.getStatus() == PostStatus.PUBLISHED) {
          sitemap.addArticle(article);
        }
      }
    }
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void deleteById(long id) {
    repository.getEntityManager().delete(Article.class, id);
    // 更新文章标签数量
    categoryService.updateArticleCount();
    refreshFeedArticles();
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void updateStatusById(PostStatus status, long id) {
    Article findById = obtainById(id);
    try (Query query = repository.createQuery(
            "UPDATE article set status = ? WHERE id = ?")) {
      query.addParameter(status);
      query.addParameter(id);
      query.executeUpdate();
    }

    categoryService.updateArticleCount(findById.getCategory());
    refreshFeedArticles();
  }

  /**
   * 保存文章
   */
  @Transactional
  public void saveArticle(Article article) {
    // save
    // TODO 保存策略
    repository.getEntityManager().persist(article);
    // save labels
    Set<Label> labels = article.getLabels();
    if (CollectionUtils.isNotEmpty(labels)) {
      labelService.saveArticleLabels(labels, article.getId());
    }

    // update category
    categoryService.updateArticleCount(article.getCategory());

    // build feed
    refreshFeedArticles();
  }

  // ----------------------------------------------------

  /**
   * 得到全部文章数目 前台
   */
  @Cacheable(key = "'countBy_'+#status")
  public int countByStatus(PostStatus status) {
    return articleRepository.getStatusRecord(status);
  }

  /***
   * 通过typeid 得到数目
   */
  @Cacheable(key = "'countCategory_'+#categoryId", unless = "#result==0")
  public int countByCategory(String categoryId) {
    return articleRepository.getRecordByCategory(categoryId);
  }

  /**
   * 得到全部文章数目
   */
  @Cacheable(key = "'count'")
  public int count() {
    return articleRepository.getTotalRecord();
  }

  /**
   * 获取最新文章
   */
  @Cacheable(key = "'latest'")
  public List<Article> getLatestArticles() {
    try (var query = repository.createQuery("""
            SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`, `update_at`
            FROM article ORDER BY id DESC LIMIT 6""")) {
      return applyLabels(query.fetch(Article.class));
    }
  }

  public Rss getRss() {
    return rss;
  }

  public Atom getAtom() {
    return atom;
  }

  public Sitemap getSitemap() {
    return sitemap;
  }

  // private

  private void applyTags(Article article) {
    if (article != null) {
      article.setLabels(labelService.getByArticleId(article.getId()));
    }
  }

  private List<ArticleItem> applyTags(List<ArticleItem> items) {
    for (ArticleItem item : items) {
      Set<Label> labels = labelService.getByArticleId(item.id);
      item.tags = labels.stream().map(Label::getName).toList();
    }
    return items;
  }

  protected int getPageNow(int pageNow, int pageSize) {
    return (pageNow - 1) * pageSize;
  }

  private List<Article> applyLabels(List<Article> ret) {
    if (CollectionUtils.isNotEmpty(ret)) {
      for (Article article : ret) {
        article.setLabels(labelService.getByArticleId(article.getId()));
      }
    }
    return ret;
  }

}
