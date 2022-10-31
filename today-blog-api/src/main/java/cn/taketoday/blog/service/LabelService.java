/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.repository.LabelRepository;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:11
 */
@Service
@CacheConfig(cacheNames = "labels"/*, expire = 1, timeUnit = TimeUnit.MINUTES*/)
public class LabelService {

  private final LabelRepository labelRepository;

  private final Cache<Long, Set<Label>> articleLabelsCache = Caffeine.newBuilder()
          .maximumSize(100)
          .expireAfterWrite(10, TimeUnit.SECONDS)
          .build();

  public LabelService(LabelRepository labelRepository) {
    this.labelRepository = labelRepository;
  }

  @Cacheable(key = "'all'")
  public List<Label> getAllLabels() {
    return labelRepository.findAll();
  }

  @Cacheable(key = "'ByName'+#name")
  public Label getByName(String name) {
    return labelRepository.findByName(name);
  }

  @Cacheable(key = "'ById'+#id")
  public Label getById(long id) {
    return labelRepository.findById(id);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void save(Label label) {
    labelRepository.save(label);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void saveAll(Collection<Label> labels) {
    labelRepository.saveAll(labels);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void deleteById(long id) {
    labelRepository.deleteById(id);
  }

  public int count() {
    return getAllLabels().size();
  }

  @SuppressWarnings("all")
  final class ArticleLabelMappingFunction implements Function<Long, Set<Label>> {

    public Set<Label> apply(Long articleId) {
      return labelRepository.findByArticleId(articleId);
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
    labelRepository.saveArticleLabels(labels, articleId);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void update(Label label) {
    labelRepository.update(label);
  }

  @Transactional
  @CacheEvict(allEntries = true)
  public void removeArticleLabels(long articleId) {

    labelRepository.removeArticleLabels(articleId);
    articleLabelsCache.invalidate(articleId);
  }

}
