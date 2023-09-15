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

  String ALERT_INFO = "alert-info";
  String ALERT_DANGER = "alert-danger";
  String ALERT_WARNING = "alert-warning";
  String ALERT_SUCCESS = "alert-success";

  String LOGIN_TYPE_QQ = "qq";
  String LOGIN_TYPE_SITE = "site";
  String LOGIN_TYPE_GITEE = "gitee";
  String LOGIN_TYPE_MASTER = "master";
  String LOGIN_TYPE_GITHUB = "github";

  String HTML_TAG = "<[^>]+>";

  /*************************************************
   * The Key Of BLOG
   */
  String KEY_RSS = "rss";
  String KEY_ATOM = "atom";
  String KEY_TITLE = "title";
  String KEY_EMAIL = "email";
  String KEY_PASSWD = "passwd";
  String KEY_AUTHOR_INFO = "author";
  String KEY_CATEGORY_NAME = "category";

  String KEY_MSG = "msg";
  String KEY_TAGS = "tags";
  String KEY_STATE = "state";
  String KEY_LABELS = "labels";
  String KEY_FORWARD = "forward";
  String KEY_SITE_MAP = "sitemap";
  String KEY_CATEGORIES = "categories";
  String KEY_OPTION = "opt";

  String KEY_CONTEXT_PATH = "contextPath";
  String KEY_ARTICLE_LIST = "article_list";

  String KEY_ID = "id";
  String KEY_HITS = "hits";
  String KEY_TOTAL = "total";
  String KEY_IMAGE = "image";
  String KEY_SUMMARY = "summary";
  String KEY_CONTENT = "content";
  String KEY_SOURCE = "_source";
  String KEY_HIGH_LIGHT = "highlight";
  String KEY_PASSWORD = "password";
  String KEY_RELEASE_DATE = "releaseDate";

  String KEY_ROOT = "root";
  String KEY_RESULT = "result";

  /*************************************************
   *
   */

  int IMG_WIDTH = 70;
  int IMG_HEIGHT = 28;

  // 文件类型
  String FILE_JS = "js";
  String FILE_CSS = "css";
  String FILE_ZIP = "zip";
  String FILE_TEXT = "txt";
  String FILE_MP3 = "mp3";
  String FILE_HTML = "html";
  String FILE_IMAGE_JPG = "jpg";
  String FILE_IMAGE_PNG = "png";
  String FILE_DIRECTORY = "directory";

  String ADMIN_ARTICLES = "articles";
  String ADMIN_COMMENTS = "comments";
  // font
  String USER_IMAGE_PATH = "headPath";

  // Session
  /** OtherFooterInfo */
  String OTHER_FOOTER_INFO = "otherFooter";
  /** CDN */
  String CDN = "cdn";
  /** 服务器启动时间 */
  String START_TIME = "startTime";
  /** 验证码 */
  String RAND_CODE = "randCode";
  String IMAGE = "/admin/today.png";
  String DEFAULT_IMAGE = "/default/default.png";

  String BLOGGER_INFO = "bloggerInfo";
  /** 登录用户 */
  String USER_INFO = "userInfo";
  /** 文章详情页面article Session */
  String ARTICLE = "article";

  String NONE_LOGIN_MSG = "{\"message\":\"登录超时\"}";

  String LOGIN_URL = "/login";
  String ERROR = "error";

  /** 默认列表大小 */
  int DEFAULT_LIST_SIZE = 10;
  int DEFAULT_MAX_PAGE_SIZE = 100;

}
