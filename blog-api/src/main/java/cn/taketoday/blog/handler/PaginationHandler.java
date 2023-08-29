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

package cn.taketoday.blog.handler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.Pagination;
import cn.taketoday.blog.model.enums.OrderBy;
import cn.taketoday.jdbc.AbstractQuery;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/19 12:57
 */
@Component
public class PaginationHandler {
  private final RepositoryManager repository;

  public PaginationHandler(RepositoryManager repository) {
    this.repository = repository;
  }

  public <T> Pagination<T> fetch(Pageable pageable, Class<T> requiredType) {
    Handler<AbstractQuery> handler = new Handler<>();
    try (JdbcConnection connection = repository.open()) {
      try (Query countQuery = connection.createQuery(
              "SELECT COUNT(id) FROM article WHERE `status` = ?")) {
        handler.queryCallback.call(countQuery);

        int count = countQuery.fetchScalar(int.class);
        if (count < 1) {
          return Pagination.empty();
        }

        String sql = """
                SELECT `id`, `uri`, `title`, `cover`, `summary`, `pv`, `create_at`
                FROM article WHERE `status` = :status
                order by create_at DESC LIMIT :pageNow, :pageSize
                """;
        try (NamedQuery query = repository.createNamedQuery(sql)) {
          handler.queryCallback.call(countQuery);

          query.addParameter("pageNow", pageNow(pageable));
          query.addParameter("pageSize", pageable.size());

          List<T> items = query.fetch(requiredType);
          return Pagination.ok(items, count, pageable);
        }
      }
    }
  }

  private static int pageNow(Pageable pageable) {
    return (pageable.current() - 1) * pageable.size();
  }

  static class Handler<Q extends AbstractQuery> {
    private final LinkedHashMap<String, OrderBy> sortKeys = new LinkedHashMap<>();

    private Pageable pageable = Pageable.of(1, BlogConstant.DEFAULT_LIST_SIZE);

    private QueryCallback<Q> queryCallback;

    public Handler<Q> config(QueryCallback<Q> queryCallback) {
      this.queryCallback = queryCallback;
      return this;
    }

    public Handler<Q> pageable(Pageable pageable) {
      this.pageable = pageable;
      return this;
    }

    /**
     * 排序
     */
    public Handler<Q> orderBy(Map<String, OrderBy> sortKeys) {
      this.sortKeys.putAll(sortKeys);
      return this;
    }

    /**
     * 排序
     *
     * @param sortKey 排序的字段
     * @param order 排序的方向
     */
    public Handler<Q> orderBy(String sortKey, OrderBy order) {
      sortKeys.put(sortKey, order);
      return this;
    }

    /**
     * 排序，默认升序
     *
     * @param sortKey 排序的字段
     */
    public Handler<Q> orderBy(String sortKey) {
      sortKeys.put(sortKey, OrderBy.ASCEND);
      return this;
    }

  }

  interface QueryCallback<T> {

    void call(T query);
  }
}

