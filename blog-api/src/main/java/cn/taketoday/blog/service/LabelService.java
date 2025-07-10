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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.blog.model.ArticleLabel;
import cn.taketoday.blog.model.Label;
import infra.cache.annotation.CacheConfig;
import infra.cache.annotation.CacheEvict;
import infra.cache.annotation.Cacheable;
import infra.lang.Nullable;
import infra.persistence.EntityManager;
import infra.persistence.EntityRef;
import infra.persistence.Where;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:11
 */
@Service
@CacheConfig(cacheNames = "labels")
public class LabelService {

  private final EntityManager entityManager;

  private final ArticleLabelMappingFunction mappingFunction = new ArticleLabelMappingFunction();

  private final Cache<Long, Set<Label>> articleLabelsCache = Caffeine.newBuilder()
          .maximumSize(100)
          .expireAfterWrite(1, TimeUnit.HOURS)
          .build();

  public LabelService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Cacheable(key = "'all'")
  public List<Label> getAllLabels() {
    return entityManager.find(Label.class);
  }

  @Nullable
  @Cacheable(key = "'ByName'+#name")
  public Label getByName(String name) {
    return entityManager.findFirst(Label.class, Map.of("name", name));
  }

  @Nullable
  @Cacheable(key = "'ById'+#id")
  public Label getById(long id) {
    return entityManager.findById(Label.class, id);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void persist(Label label) {
    entityManager.persist(label);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void persist(Collection<Label> labels) {
    entityManager.persist(labels);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void deleteById(long id) {
    entityManager.delete(Label.class, id);
  }

  public int count() {
    return getAllLabels().size();
  }

  public Set<Label> getByArticleId(long id) {
    return articleLabelsCache.get(id, mappingFunction);
  }

  public Set<String> getAllLabelsNames() {
    return getAllLabels()
            .stream()
            .map(Label::getName)
            .collect(Collectors.toSet());
  }

  @CacheEvict(allEntries = true)
  public void persistArticleLabels(Set<Label> labels, long articleId) {
    entityManager.persist(labels.stream().map(label -> ArticleLabel.of(label.getId(), articleId)));
    articleLabelsCache.invalidate(articleId);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void updateById(Label label) {
    entityManager.updateById(label);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void removeArticleLabels(long articleId) {
    entityManager.delete(ArticleLabel.forArticle(articleId));
    articleLabelsCache.invalidate(articleId);
  }

  final class ArticleLabelMappingFunction implements Function<Long, Set<Label>> {

    @Override
    public Set<Label> apply(Long articleId) {
      return new LinkedHashSet<>(entityManager.find(Label.class, new TagQuery(articleId)));
    }

  }

  @EntityRef(Label.class)
  static class TagQuery {

    @Where("`id` IN (SELECT `label_id` FROM article_label WHERE `article_id` = ? )")
    public final Long articleId;

    TagQuery(Long articleId) {
      this.articleId = articleId;
    }
  }

}
