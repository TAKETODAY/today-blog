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

package cn.taketoday.blog;

import cn.taketoday.lang.VersionExtractor;

/**
 * 时间:2018,1,6 2018 1 16
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:09 s
 */
public interface BlogConstant extends cn.taketoday.lang.Constant {
  String applicationName = "today-blog-api";

  String version = VersionExtractor.forClass(BlogConstant.class);

  // default font
  String DEFAULT_FONT = "Verdana";
  int BUFFER_SIZE = 8 * 1024;

  long STARTUP_TIME = System.currentTimeMillis();

  String PARAMETER_SIZE = "size";
  String PARAMETER_CURRENT = "page";

  String OPERATION_OK = "ok";
  String OPERATION_FAILED = "操作失败";

  String SITE_ICP = "site.icp";
  String SITE_CDN = "site.cdn";
  String SITE_NAME = "site.name";
  String SITE_HOST = "site.host";
  String SITE_KEYWORDS = "site.keywords";
  String SITE_COPYRIGHT = "site.copyright";
  String SITE_DESCRIPTION = "site.description";
  String SITE_LIST_SIZE = "site.listSize";
  String SITE_MAX_PAGE_SIZE = "site.maxPageSize";
  String SITE_COMMENT_LIST_SIZE = "comment.list.size";
  String SITE_ARTICLE_FEED_LIST_SIZE = "article.feed.list.size";
  String SITE_COMMENT_CONTENT_LENGTH = "comment.content.length";

  String LOGIN_TYPE_QQ = "qq";
  String LOGIN_TYPE_SITE = "site";
  String LOGIN_TYPE_GITEE = "gitee";
  String LOGIN_TYPE_MASTER = "master";
  String LOGIN_TYPE_GITHUB = "github";

  String HTML_TAG = "<[^>]+>";

  String KEY_STATE = "state";

  // Session
  /** CDN */
  String CDN = "cdn";
  /** 验证码 */
  String RAND_CODE = "randCode";

  /** 默认列表大小 */
  int DEFAULT_LIST_SIZE = 10;
  int DEFAULT_MAX_PAGE_SIZE = 100;

}
