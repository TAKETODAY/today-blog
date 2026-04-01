/*
 * Copyright 2017 - 2026 the original author or authors.
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

import cn.taketoday.blog.log.LoggingPersister;
import cn.taketoday.blog.model.OperationLogging;
import cn.taketoday.blog.web.Pageable;
import cn.taketoday.blog.web.Pagination;
import infra.persistence.EntityManager;
import infra.stereotype.Service;
import infra.transaction.annotation.Transactional;

/**
 * 日志服务
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-16 18:38
 */
@Service
public class LoggingService implements LoggingPersister {

  private final EntityManager entityManager;

  public LoggingService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void persist(OperationLogging operation) {
    entityManager.persist(operation);
  }

  /**
   * 查询最新的五条日志
   *
   * @return List
   */
  public List<OperationLogging> getLatest() {
    return entityManager.find(OperationLogging.class, Queries.forSelect(select -> select.limit(5)
            .orderBy()
            .desc("id")));
  }

  public void truncateTable() {
    entityManager.truncate(OperationLogging.class);
  }

  public void deleteById(long id) {
    entityManager.delete(OperationLogging.class, id);
  }

  @Transactional
  public void deleteByIds(long[] idArray) {
    for (long id : idArray) {
      entityManager.delete(OperationLogging.class, id);
    }
  }

  public List<OperationLogging> getAll() {
    // select * FROM logging
    return entityManager.find(OperationLogging.class);
  }

  public Pagination<OperationLogging> pagination(Pageable pageable) {
    return Pagination.from(entityManager.page(OperationLogging.class, pageable));
  }

  private static int pageNow(Pageable pageable) {
    return (pageable.pageNumber() - 1) * pageable.pageSize();
  }

}
