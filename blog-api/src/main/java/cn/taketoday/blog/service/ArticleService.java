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

import cn.taketoday.blog.BlogConstant;
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
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@Service
@CustomLog
@RequiredArgsConstructor
@CacheConfig(cacheNames = "Articles")
public class ArticleService {
  private final BlogConfig blogConfig;
  private final LabelService labelService;
  private final CategoryService categoryService;
  private final ArticleRepository articleRepository;
  private final RepositoryManager repositoryManager;

  private final Rss rss = new Rss();
  private final Atom atom = new Atom();
  private final Sitemap sitemap = new Sitemap();

  /**
   * Update {@link Article}
   *
   * @param article article instance
   */
  @Async
  @Transactional
  @CacheEvict(key = "'ById_'+#article.id")
  public void update(Article article) {
    Article oldArticle = obtainById(article.getId());

    articleRepository.update(article);

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
      // update labels
      updateArticleLabels(article, oldArticle);
    }
    catch (Exception e) {
      throw InternalServerException.failed("文章标签更新失败", e);
    }

    try {
      buildFeed();
    }
    catch (Exception e) {
      throw InternalServerException.failed("文章订阅更新失败", e);
    }
  }

  protected void updateArticleLabels(Article newArticle, Article articleInDb) {

    Set<Label> oldLabels = articleInDb.getLabels();
    Set<Label> newLabels = newArticle.getLabels();
    if (labelsChanged(newLabels, oldLabels)) {
      if (!CollectionUtils.isEmpty(oldLabels)) {
        labelService.removeArticleLabels(newArticle.getId());
      }
      if (!CollectionUtils.isEmpty(newLabels)) {
        labelService.saveArticleLabels(newLabels, newArticle.getId());
      }
    }
  }

  /**
   * If {@link Label}s changed
   */
  protected boolean labelsChanged(Set<Label> newLabels, Set<Label> oldLabels) {
    if (CollectionUtils.isEmpty(newLabels)) {
      return !CollectionUtils.isEmpty(oldLabels);
    }
    return !Objects.equals(newLabels, oldLabels);
  }

  public void updatePageView(long id) {
    articleRepository.updatePageView(id);
  }

  @Cacheable(key = "'ById_'+#id")
  public Article getById(long id) {
    Article findById = articleRepository.findById(id);
    return findById == null ? null : findById.setLabels(labelService.getByArticleId(id));
  }

  @Nullable
  @Cacheable(key = "'getByURI_'+#uri")
  public Article getByURI(String uri) {
    Assert.notNull(uri, "文章地址不能为空");
    // language=MySQL
    try (Query query = repositoryManager.createQuery("SELECT * FROM article WHERE uri=? LIMIT 1")) {
      query.addParameter(uri);

      Article article = query.fetchFirst(Article.class);
      if (article != null) {
        article.setLabels(labelService.getByArticleId(article.getId()));
      }
      return article;
    }
  }

  /**
   * @return {@link Article} never be null
   */
  protected Article obtainById(long id) {
    Article byId = getById(id);
    if (byId == null) {
      throw new NotFoundException("该文章不存在或已删除不能操作");
    }
    return byId;
  }

  public List<Article> getMostPopularArticles() {
    return articleRepository.findByClickHit(BlogConstant.DEFAULT_LIST_SIZE);
  }

  public List<Article> getFeedArticles() {
    int listSize = blogConfig.getArticleFeedListSize();
    return applyLabels(articleRepository.getFeedArticles(listSize));
  }

  /**
   * Build feed
   *
   * @see cn.taketoday.beans.factory.InitializingBean#afterPropertiesSet()
   */
  @PostConstruct
  public void buildFeed() {

    List<Article> feedArticles = getFeedArticles();

    buildAtom(feedArticles);
    buildRss(feedArticles);

    buildSitemap();
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

      item.addCategories(article.getLabels()//
              .stream()//
              .map(Label::getName)//
              .collect(Collectors.toSet())//
      );
      rss.addItem(item);
    }

    rss.setLastBuildDate(System.currentTimeMillis());
  }

  protected void buildSitemap() {
    log.debug("Build Sitemap");
    sitemap.getUrls().clear();

    for (Article article : getAll()) {
      if (article.getStatus() == PostStatus.PUBLISHED) {
        sitemap.addUrl(Sitemap.newURL(article));
      }
    }
  }

  public List<Article> getAll() {
    return articleRepository.findAll();
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

  @Transactional
  @CacheEvict(allEntries = true)
  public void deleteById(long id) {

    articleRepository.deleteById(id);
    // update count
    categoryService.updateArticleCount();

    buildFeed();
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void updateStatusById(PostStatus status, long id) {

    Article findById = obtainById(id);

    articleRepository.updateStatus(status, id);
    categoryService.updateArticleCount(findById.getCategory());

    buildFeed();
  }

  /**
   * find home page articles
   */
//  @Cacheable(key = "'home-'+#pageable.getCurrent()+'-'+#pageable.getSize()")
  public List<ArticleItem> getHomeArticles(Pageable pageable) {
    int pageSize = pageable.getSize();
    int current = pageable.getCurrent();
    // language=MySQL
    String sql = """
            SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
            FROM article
            WHERE `status` = :status
            order by create_at DESC
            LIMIT :pageNow, :pageSize
            """;

    try (NamedQuery query = repositoryManager.createNamedQuery(sql)) {
      query.addParameter("pageNow", getPageNow(current, pageSize))
              .addParameter("status", PostStatus.PUBLISHED)
              .addParameter("pageSize", pageSize);

      List<ArticleItem> items = query.fetch(ArticleItem.class);
      for (ArticleItem item : items) {
        Set<Label> labels = labelService.getByArticleId(item.getId());
        item.setTags(labels.stream().map(Label::getName).toList());
      }
      return items;
    }
  }

//  @Cacheable
//  public List<Article> getHomeArticles(Pageable pageable) {
//    return getIndexArticles(pageable.getCurrent(), pageable.getSize());
//  }
//
//  public List<Article> getIndexArticles(int pageNow, int pageSize) {
//    return applyLabels(articleRepository.findIndexArticles(getPageNow(pageNow, pageSize), pageSize));
//  }

  /**
   * 保存文章
   */
  @Transactional
  public void saveArticle(Article article) {
    // save
    repositoryManager.persist(article);
    // save labels
    Set<Label> labels = article.getLabels();
    if (CollectionUtils.isNotEmpty(labels)) {
      labelService.saveArticleLabels(labels, article.getId());
    }

    // update category
    categoryService.updateArticleCount(article.getCategory());

    // build feed
    buildFeed();
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
   * 通过tag获取数目
   */
  @Cacheable(key = "'countByLabel_'+#tag", unless = "#result==0")
  public int countByLabel(String tag) {
    return articleRepository.getRecordByLabel(tag);
  }

  /**
   * 得到全部文章数目
   */
  @Cacheable(key = "'count'")
  public int count() {
    return articleRepository.getTotalRecord();
  }

  /**
   * 根据tag和分页获取文章
   */
  @Cacheable(key = "'tag_'+#tag+'_'+#pageNow+'_'+#pageSize")
  public List<Article> getByLabel(String tag, int pageNow, int pageSize) {
    return applyLabels(articleRepository.findArticlesByLabel(getPageNow(pageNow, pageSize), pageSize, tag));
  }

  /***
   * 根据类型找文章
   */
  @Cacheable(key = "'cate_'+#name+'_'+#pageNow+'_'+#pageSize")
  public List<Article> getByCategory(String name, int pageNow, int pageSize) {
    return applyLabels(articleRepository.findArticlesByCategory(getPageNow(pageNow, pageSize), pageSize, name));
  }

  @Cacheable(key = "'all_'+#status+'_'+#pageNow+'_'+#pageSize")
  public List<Article> getByStatus(PostStatus status, int pageNow, int pageSize) {
    return applyLabels(articleRepository.findByStatus(status, getPageNow(pageNow, pageSize), pageSize));
  }

  @Cacheable(key = "'latest'")
  public List<Article> getLatest() {
    return articleRepository.findLatest();
  }

  public List<Article> get(int pageNow, int pageSize) {
    return applyLabels(articleRepository.find(getPageNow(pageNow, pageSize), pageSize));
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

  //
  public Pagination<Article> search(String q, Pageable pageable) {
    int count = articleRepository.getSearchRecord(q);
    if (count < 1) {
      return Pagination.empty();
    }
    List<Article> articles = articleRepository.search(q,
            getPageNow(pageable.getCurrent(), pageable.getSize()),
            pageable.getSize());
    return Pagination.ok(articles, count, pageable);
  }

  public Pagination<Article> search(SearchForm from, Pageable pageable) {
    int count = articleRepository.getRecord(from);
    if (count < 1) {
      return Pagination.empty();
    }
    List<Article> articles = articleRepository.find(from,
            getPageNow(pageable.getCurrent(), pageable.getSize()),
            pageable.getSize());
    return Pagination.ok(articles, count, pageable);
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

  public List<Article> get(Pageable pageable) {
    return get(pageable.getCurrent(), pageable.getSize());
  }

  public List<Article> getByStatus(PostStatus status, Pageable pageable) {
    return getByStatus(status, pageable.getCurrent(), pageable.getSize());
  }

  public List<Article> getByCategory(String categoryName, Pageable pageable) {
    return getByCategory(categoryName, pageable.getCurrent(), pageable.getSize());
  }

  public List<Article> getByLabel(String label, Pageable pageable) {
    return getByLabel(label, pageable.getCurrent(), pageable.getSize());
  }

}
