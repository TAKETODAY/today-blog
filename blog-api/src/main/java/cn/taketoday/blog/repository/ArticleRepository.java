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

package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.model.form.SearchForm;
import cn.taketoday.stereotype.Repository;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-10 19:40
 */
@Repository
public interface ArticleRepository extends DefaultRepository<Article, Long> {

  /**
   * find the index page's articles
   */
  List<Article> findIndexArticles(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  /**
   *
   */
  List<Article> findArticlesByLabel(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize,
          @Param("name") String name);

  /**
   *
   */
  List<Article> findArticlesByCategory(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize,
          @Param("category") String category);

  /**
   *
   */
  List<Article> findByClickHit(@Param("pageSize") int defaultListSize);

  /**
   * click hit update
   */
  @Update("update article set `pv`=pv+1 where `id` = #{id}")
  void updatePageView(@Param("id") long id);

  /**
   *
   */
  @ResultMap("BaseResultMap")
  @Select("SELECT * FROM article WHERE status=0 order by id DESC LIMIT 0, #{size}")
  List<Article> getFeedArticles(@Param("size") int size);

  // ---------------------------------------------------------
  int getStatusRecord(@Param("status") PostStatus status);

  int getRecordByCategory(String category);

  /**
   * 内连接获取数目
   */
  int getRecordByLabel(String name);

  List<Article> findLatest();

  @Update("UPDATE article set status = #{status.code} WHERE id = #{id}")
  void updateStatus(@Param("status") PostStatus status, @Param("id") long id);

  List<Article> findByStatus(@Param("status") PostStatus status,
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  List<Article> search(@Param("query") String q,
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  int getSearchRecord(@Param("query") String q);

  List<Article> find(@Param("args") SearchForm args,
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  int getRecord(@Param("args") SearchForm args);

}
