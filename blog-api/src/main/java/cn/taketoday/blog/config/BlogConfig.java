/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.blog.config;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ConfigBinding;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:08
 */
@Setter
@Getter
@Component
@ConfigBinding("site.")
@ConfigurationProperties(prefix = "site")
public class BlogConfig {

  private String cdn;
  private String icp;
  private String host;
  private String name;
  private String index;
  private String upload;
  private String keywords;
  private String copyright;
  private String subTitle;
  private String serverPath;
  private String description;
  private String otherFooter;

  @ConfigBinding("author.email")
  private String email;

  private final long startupTimeMillis = System.currentTimeMillis();

  @ConfigBinding(value = "article.feed.list.size", splice = false)
  private int articleFeedListSize = 10;

  @ConfigBinding("list.size")
  private int listSize = BlogConstant.DEFAULT_LIST_SIZE; // articles list size

  @ConfigBinding("max.list.size")
  private int maxPageSize = BlogConstant.DEFAULT_MAX_PAGE_SIZE;

}
