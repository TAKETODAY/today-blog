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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.model.Category;
import cn.taketoday.blog.repository.CategoryRepository;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.framework.context.event.ApplicationStartedEvent;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-02 21:02
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "categories")
public class CategoryService {
  private final RepositoryManager repositoryManager;
  private final CategoryRepository categoryRepository;

  @CacheEvict(allEntries = true)
  public void save(Category category) {
    categoryRepository.save(category);
  }

  @Cacheable(key = "'all'")
  public List<Category> getAllCategories() {
    return categoryRepository.findAll();
  }

  @Cacheable(key = "'ByName'+#name")
  public Category getCategory(String name) {
    return categoryRepository.findById(name);
  }

  /**
   * 更新对应文章分类的数量
   *
   * @param categoryName 分类名称
   */
  @CacheEvict(allEntries = true)
  public void updateArticleCount(String categoryName) {
    try (NamedQuery query = repositoryManager.createNamedQuery("""
            UPDATE category SET articleCount = (
                SELECT COUNT(id) FROM article WHERE status = 0 and category =:name
            ) WHERE name = :name""")) {
      // language=
      query.addParameter("name", categoryName);
      query.executeUpdate();
    }
  }

  /**
   * 更新所有的分类对应文章数量
   */
  @Transactional
  @CacheEvict(allEntries = true)
  @EventListener(ApplicationStartedEvent.class)
  public void updateArticleCount() {
    try (Query query = repositoryManager.createQuery("""
            UPDATE category SET articleCount =
                    (SELECT COUNT(id)
                     FROM article
                     WHERE status = 0
                       and category = name)
            WHERE name in ((select names from (select name names from category) as a))""")) {
      query.executeUpdate();
    }
  }

  public void delete(Category category) {
    delete(category.getName());
  }

  @CacheEvict(allEntries = true)
  public void delete(String name) {
    categoryRepository.deleteById(name);
  }

  @CacheEvict(allEntries = true)
  public void update(Category category, String name) {

    Map<String, Object> map = new HashMap<>();

    map.put("id", name);
    map.put("name", category.getName());
    map.put("order", category.getOrder());
    map.put("description", category.getDescription());

    categoryRepository.updateById(map);
  }

}
