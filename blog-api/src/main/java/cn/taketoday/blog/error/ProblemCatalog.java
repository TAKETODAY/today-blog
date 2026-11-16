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

package cn.taketoday.blog.error;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.blog.maintenance.backup.BackupProblems;
import cn.taketoday.blog.maintenance.upgrade.UpgradeProblems;
import cn.taketoday.blog.service.ArticleProblems;
import cn.taketoday.blog.service.ContentProblems;
import cn.taketoday.blog.service.UserProblems;

/**
 * 显式汇总协议清单供校验与文档使用，不参与请求时注册。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2026/11/16
 */
public final class ProblemCatalog {

  private ProblemCatalog() {
  }

  public static List<Problem> all() {
    return Stream.of(CommonProblems.values(), ArticleProblems.values(), ContentProblems.values(),
                    UserProblems.values(), UpgradeProblems.values(), BackupProblems.values())
            .flatMap(Arrays::stream)
            .map(problem -> (Problem) problem)
            .toList();
  }
}
