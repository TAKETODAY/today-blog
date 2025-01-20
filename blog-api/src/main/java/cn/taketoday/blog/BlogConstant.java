/*
 * Copyright 2017 - 2025 the original author or authors.
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

package cn.taketoday.blog;

import infra.lang.VersionExtractor;

/**
 * 时间:2018,1,6 2018 1 16
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:09 s
 */
public interface BlogConstant extends infra.lang.Constant {

  String version = VersionExtractor.forClass(BlogConstant.class);

  // default font
  String DEFAULT_FONT = "Verdana";

  String PARAMETER_SIZE = "size";
  String PARAMETER_CURRENT = "page";

  String LOGIN_TYPE_QQ = "qq";
  String LOGIN_TYPE_SITE = "site";
  String LOGIN_TYPE_GITEE = "gitee";
  String LOGIN_TYPE_MASTER = "master";
  String LOGIN_TYPE_GITHUB = "github";

  String HTML_TAG = "<[^>]+>";

  int DEFAULT_LIST_SIZE = 10;

  int DEFAULT_MAX_PAGE_SIZE = 100;

}
