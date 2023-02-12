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

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.blog.ErrorMessageException;
import cn.taketoday.blog.model.enums.StatisticsField;
import cn.taketoday.blog.model.form.PageViewStatistics;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.core.JdbcOperations;
import cn.taketoday.jdbc.core.PreparedStatementCallback;
import cn.taketoday.stereotype.Service;
import cn.taketoday.web.InternalServerException;
import lombok.RequiredArgsConstructor;

/**
 * @author TODAY 2021/2/8 22:02
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

  //  private final Cache cache = Caffeine.newBuilder()
//          .maximumSize(3)
//          .expireAfterWrite(10, TimeUnit.SECONDS) // 10s
//          .build();
  private final JdbcOperations template;
  private final RepositoryManager repositoryManager;
  final CacheManager cacheManager;

  public Map<String, Integer> analyze(StatisticsField type) {
    String name = type.name();
    String sql = "SELECT `" + name + "`, COUNT(`" + name + "`) " +
            "from page_view where `" + name + "` is not null GROUP BY `" + name + "`";
    try {
      return template.execute(sql, (PreparedStatementCallback<Map<String, Integer>>) ps -> {
        Map<String, Integer> ret = new HashMap<>();
        try (final ResultSet result = ps.executeQuery()) {
          while (result.next()) {
            String key = result.getString(1);
            int count = result.getInt(2);
            ret.put(key, count);
          }
        }
        return ret;
      });
    }
    catch (Exception e) {
      throw InternalServerException.failed("查询出错", e);
    }
  }

  public Map<String, PageViewStatistics> analyzePageView(LocalDate from, LocalDate to) {
    boolean hasPeriod = hasPeriod(from, to);
    String sql;
    if (hasPeriod) {
      sql = "SELECT DATE_FORMAT(create_at, '%Y-%m-%d') days, COUNT(DISTINCT `ip`), COUNT(user != 0), COUNT(*) " +
              "from page_view where create_at between ? and ? GROUP BY `days`";
    }
    else {
      sql = "SELECT DATE_FORMAT(create_at, '%Y-%m-%d') days, COUNT(DISTINCT `ip`), COUNT(user != 0), COUNT(*) " +
              "from page_view GROUP BY `days`";
    }

    try {
      return template.execute(sql, (PreparedStatementCallback<Map<String, PageViewStatistics>>) ps -> {
        if (hasPeriod) {
          ps.setObject(1, from);
          ps.setObject(2, to);
        }

        LinkedHashMap<String, PageViewStatistics> statistics = new LinkedHashMap<>();

        try (ResultSet result = ps.executeQuery()) {
          while (result.next()) {
            PageViewStatistics viewStatistics = new PageViewStatistics();
            viewStatistics.ip = result.getInt(2);
            viewStatistics.uv = result.getInt(3);
            viewStatistics.pv = result.getInt(4);
            String key = result.getString(1);

            statistics.put(key, viewStatistics);
          }
        }

        return statistics;
      });
    }
    catch (Exception e) {
      throw InternalServerException.failed("查询出错", e);
    }
  }

  public static boolean hasPeriod(LocalDate from, LocalDate to) {
    if (from != null && to != null) {
      //检查时间是否合法
      if (to.isBefore(from)) {
        throw ErrorMessageException.failed("起始日期应该小于终止日期");
      }
      else {
        return true;
      }
    }
    else {
      return false;
    }
  }

}

