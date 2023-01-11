package cn.taketoday.blog.service;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.blog.ApplicationException;
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
      if (to.compareTo(from) < 0) {
        throw ApplicationException.failed("起始日期应该小于终止日期");
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

