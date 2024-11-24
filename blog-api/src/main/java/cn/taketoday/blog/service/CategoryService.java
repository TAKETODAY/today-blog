/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.util.List;
import java.util.Map;

import cn.taketoday.blog.model.Category;
import infra.app.context.event.ApplicationStartedEvent;
import infra.context.event.EventListener;
import infra.jdbc.NamedQuery;
import infra.jdbc.Query;
import infra.jdbc.RepositoryManager;
import infra.lang.Nullable;
import infra.persistence.EntityManager;
import infra.persistence.Order;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-02 21:02
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

  private final EntityManager entityManager;

  private final RepositoryManager repository;

  public void save(Category category) {
    entityManager.persist(category, false);
  }

  public List<Category> getAllCategories() {
    return entityManager.find(Category.class, Map.of("order", Order.ASC));
  }

  @Nullable
  public Category getCategory(String name) {
    return entityManager.findById(Category.class, name);
  }

  /**
   * 更新对应文章分类的数量
   *
   * @param categoryName 分类名称
   */
  public void updateArticleCount(String categoryName) {
    try (NamedQuery query = repository.createNamedQuery("""
            UPDATE category SET article_count = (
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
  @EventListener(ApplicationStartedEvent.class)
  public void updateArticleCount() {
    try (Query query = repository.createQuery("""
            UPDATE category SET article_count =
                    (SELECT COUNT(id)
                     FROM article
                     WHERE status = 0
                       and category = name)
            WHERE name in ((select names from (select name names from category) as a))""")) {
      query.executeUpdate();
    }
  }

  /**
   * 根据 name 删除
   *
   * @param name id
   */
  public void deleteById(String name) {
    entityManager.delete(Category.class, name);
  }

  /**
   * 根据 name 更新 信息
   *
   * @param category 更新的数据体
   * @param name id
   */
  public void updateById(Category category, String name) {
    entityManager.updateById(category, name);
  }

}
