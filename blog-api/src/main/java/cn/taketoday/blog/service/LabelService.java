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

package cn.taketoday.blog.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.blog.model.ArticleLabel;
import cn.taketoday.blog.model.Label;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.EntityManager;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;

import static cn.taketoday.persistence.QueryCondition.isEqualsTo;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:11
 */
@Service
@CacheConfig(cacheNames = "labels"/*, expire = 1, timeUnit = TimeUnit.MINUTES*/)
public class LabelService {

  private final EntityManager entityManager;

  private final RepositoryManager repository;

  private final Cache<Long, Set<Label>> articleLabelsCache = Caffeine.newBuilder()
          .maximumSize(100)
          .expireAfterWrite(10, TimeUnit.SECONDS)
          .build();

  public LabelService(EntityManager entityManager, RepositoryManager repository) {
    this.entityManager = entityManager;
    this.repository = repository;
  }

  @Cacheable(key = "'all'")
  public List<Label> getAllLabels() {
    return entityManager.find(Label.class);
  }

  @Nullable
  @Cacheable(key = "'ByName'+#name")
  public Label getByName(String name) {
    return entityManager.findFirst(Label.class, isEqualsTo("name", name));
  }

  @Nullable
  @Cacheable(key = "'ById'+#id")
  public Label getById(long id) {
    return entityManager.findById(Label.class, id);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void save(Label label) {
    entityManager.persist(label);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void saveAll(Collection<Label> labels) {
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

  final class ArticleLabelMappingFunction implements Function<Long, Set<Label>> {

    public Set<Label> apply(Long articleId) {
      return new LinkedHashSet<>(repository.createQuery("""
                      SELECT * FROM label WHERE `id` IN (SELECT `labelId` FROM article_label WHERE `articleId` = ? )""")
              .addParameter(articleId)
              .fetch(Label.class));
    }
  }

  final ArticleLabelMappingFunction mappingFunction = new ArticleLabelMappingFunction();

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
  public void saveArticleLabels(Set<Label> labels, long articleId) {
    entityManager.persist(labels.stream().map(label -> ArticleLabel.of(label.getId(), articleId)));
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void updateById(Label label) {
    entityManager.updateById(label);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void removeArticleLabels(long articleId) {
    try (Query query = repository.createQuery("""
            DELETE from article_label WHERE articleId = ?""")) {
      query.addParameter(articleId);
      query.executeUpdate();
    }

    articleLabelsCache.invalidate(articleId);
  }

}
