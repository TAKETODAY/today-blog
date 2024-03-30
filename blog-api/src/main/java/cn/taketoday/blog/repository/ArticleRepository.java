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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;

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

  int getStatusRecord(@Param("status") PostStatus status);

  int getRecordByCategory(String category);

  /**
   * 内连接获取数目
   */
  int getRecordByLabel(String name);

  List<Article> findLatest();

  List<Article> find(@Param("args") SearchForm args,
          @Param("pageNow") int offset, @Param("pageSize") int pageSize);

  int getRecord(@Param("args") SearchForm args);

}
