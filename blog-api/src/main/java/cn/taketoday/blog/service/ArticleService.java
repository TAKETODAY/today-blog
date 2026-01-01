/*
 * Copyright 2017 - 2025 the original author or authors.
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

package cn.taketoday.blog.service;

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import cn.taketoday.blog.model.form.ArticleConditionForm;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import infra.beans.factory.InitializingBean;
import infra.cache.annotation.CacheConfig;
import infra.cache.annotation.CacheEvict;
import infra.cache.annotation.Cacheable;
import infra.jdbc.JdbcConnection;
import infra.jdbc.Query;
import infra.jdbc.RepositoryManager;
import infra.lang.Assert;
import infra.persistence.EntityManager;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityRef;
import infra.persistence.Order;
import infra.persistence.OrderBy;
import infra.persistence.SimpleSelectQueryStatement;
import infra.persistence.Transient;
import infra.persistence.sql.SimpleSelect;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;
import infra.util.CollectionUtils;
import infra.web.server.InternalServerException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@Service
@CustomLog
@RequiredArgsConstructor
@CacheConfig(cacheNames = "Articles")
public class ArticleService implements InitializingBean {

  private final BlogConfig blogConfig;

  private final LabelService labelService;

  private final EntityManager entityManager;

  private final RepositoryManager repository;

  private final CategoryService categoryService;

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

    entityManager.updateById(article);

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
        labelService.persistArticleLabels(newLabels, newArticle.getId());
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

  @Nullable
  @Cacheable(key = "'ById_'+#id")
  public Article getById(long id) {
    Article article = entityManager.findById(Article.class, id);
    applyTags(article);
    return article;
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
  public Article obtainById(long id) {
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
      query.addParameter(pageable.pageSize(20));
      return applyTags(query.fetch(ArticleItem.class));
    }
  }

  /**
   * 获取首页文章
   */
  @Cacheable(key = "'home-'+#pageable.pageNumber()+'-'+#pageable.pageSize()")
  public Pagination<ArticleItem> getHomeArticles(Pageable pageable) {
    return entityManager.page(ArticleItem.class, new ArticleStatus(PostStatus.PUBLISHED), pageable)
            .peek(this::applyTags)
            .map(Pagination::from);
  }

  /**
   * 搜索文章
   */
  public Pagination<ArticleItem> search(String q, Pageable pageable) {
    ArticleConditionForm form = new ArticleConditionForm();
    form.setQ(q);
    return entityManager.page(ArticleItem.class, form, pageable)
            .peek(this::applyTags)
            .map(Pagination::from);
  }

  public Pagination<Article> search(ArticleConditionForm from, Pageable pageable) {
    return entityManager.page(Article.class, from, pageable)
            .map(page -> Pagination.ok(page.getRows(), page.getTotalRows().intValue(), pageable));
  }

  /**
   * 更具标签获取对应文章
   */
  public Pagination<ArticleItem> getArticlesByTag(String label, Pageable pageable) {
    try (JdbcConnection connection = repository.open()) {
      try (var countQuery = connection.createNamedQuery("""
              SELECT COUNT(*) FROM article
              LEFT JOIN article_label ON article.id = article_label.article_id
              WHERE status = :status
                and article_label.label_id IN (
                  SELECT label_id FROM article_label
                    WHERE label_id = (SELECT id FROM label WHERE name = :name))""")) {

        countQuery.addParameter("name", label);
        countQuery.addParameter("status", PostStatus.PUBLISHED);
        int count = countQuery.scalar(int.class);
        if (count < 1) {
          return Pagination.empty();
        }
        try (var dataQuery = connection.createNamedQuery("""
                SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
                FROM article LEFT JOIN article_label ON article.id = article_label.article_id
                WHERE article.status = :status
                  AND article_label.label_id IN (
                    SELECT label_id FROM article_label
                      WHERE label_id = (SELECT id FROM label WHERE name = :name)
                  )
                LIMIT :offset, :size""")) {

          dataQuery.addParameter("name", label);
          dataQuery.addParameter("status", PostStatus.PUBLISHED);
          dataQuery.addParameter("offset", pageable.offset());
          dataQuery.addParameter("size", pageable.pageSize());
          List<ArticleItem> items = applyTags(dataQuery.fetch(ArticleItem.class));
          return Pagination.ok(items, count, pageable);
        }
      }
    }
  }

  /***
   * 根据类型找文章
   * @param pageable 分页
   */
  @Cacheable(key = "'cate_'+#categoryName+'_'+#pageable.offset()")
  public Pagination<ArticleItem> getArticlesByCategory(String categoryName, Pageable pageable) {
    return entityManager.page(ArticleItem.class, Map.of("status", PostStatus.PUBLISHED, "category", categoryName), pageable)
            .peek(this::applyTags)
            .map(Pagination::from);
  }

  /**
   * 刷新订阅文章
   */
  public void refreshFeedArticles() {
    var feedArticles = entityManager.find(Article.class, new FeedArticles(blogConfig.articleFeedListSize));
    applyLabels(feedArticles);

    buildAtom(feedArticles);
    buildRss(feedArticles);
    buildSitemap();
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
    atom.clear();

    for (Article article : feedArticles) {
      if (article.needPassword()) {
        continue;
      }

      Entry entry = new Entry();
      entry.setId(article.getId());
      entry.setUri(article.getUri());
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
    rss.clear();

    for (Article article : feedArticles) {
      if (article.needPassword()) {
        continue;
      }

      Item item = new Item();
      item.setId(article.getId());
      item.setUri(article.getUri());
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

  protected void buildSitemap() {
    log.debug("Build Sitemap");
    sitemap.clear();

    for (Article article : entityManager.find(Article.class, Map.of("create_at", Order.DESC))) {
      if (article.getStatus() == PostStatus.PUBLISHED) {
        sitemap.addArticle(article);
      }
    }
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void deleteById(long id) {
    entityManager.delete(Article.class, id);
    // 更新文章标签数量
    categoryService.updateArticleCount();
    refreshFeedArticles();
  }

  @CacheEvict(allEntries = true)
  public void updateStatusById(PostStatus status, long id) {
    Assert.notNull(status, "status is required");
    Article findById = obtainById(id);

    repository.executeWithoutResult(txStatus -> {
      entityManager.updateById(new ArticleStatus(status), id);
      categoryService.updateArticleCount(findById.getCategory());
    });

    refreshFeedArticles();
  }

  /**
   * 保存文章
   */
  @Transactional
  public void saveArticle(Article article) {
    // save
    // TODO 保存策略
    entityManager.persist(article);
    // save labels
    Set<Label> labels = article.getLabels();
    if (CollectionUtils.isNotEmpty(labels)) {
      labelService.persistArticleLabels(labels, article.getId());
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
    return entityManager.count(Article.class, Map.of("status", status)).intValue();
  }

  /***
   * 通过typeid 得到数目
   */
  @Cacheable(key = "'countCategory_'+#category", unless = "#result==0")
  public int countByCategory(String category) {
    ArticleConditionForm form = new ArticleConditionForm();
    form.setCategory(category);
    form.setStatus(PostStatus.PUBLISHED);
    return entityManager.count(Article.class, form).intValue();
  }

  /**
   * 得到全部文章数目
   */
  @Cacheable(key = "'count'")
  public int count() {
    return entityManager.count(Article.class).intValue();
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

  private void applyTags(@Nullable Article article) {
    if (article != null) {
      article.setLabels(labelService.getByArticleId(article.getId()));
    }
  }

  private void applyTags(@Nullable ArticleItem item) {
    if (item != null) {
      Set<Label> labels = labelService.getByArticleId(item.id);
      item.tags = labels.stream().map(Label::getName).toList();
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

  @OrderBy("create_at DESC")
  @EntityRef(Article.class)
  static class ArticleStatus {

    public final PostStatus status;

    ArticleStatus(PostStatus status) {
      this.status = status;
    }
  }

  static class FeedArticles extends SimpleSelectQueryStatement {

    @Transient
    public final int limit;

    public FeedArticles(int limit) {
      this.limit = limit;
    }

    @Override
    protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
      select.addRestriction("status");
      select.limit(limit);
      select.orderBy("id", Order.DESC);
    }

    @Override
    public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
      statement.setInt(1, PostStatus.PUBLISHED.getValue());
    }
  }

}
