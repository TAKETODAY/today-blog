/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.blog.config;

import cn.taketoday.blog.BlogConstant;
import cn.taketoday.blog.ConfigBinding;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.stereotype.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-16 21:08
 */
@Component
@ConfigBinding("site.")
@ConfigurationProperties(prefix = "site")
public class BlogConfig {

  public String cdn;

  public String icp;

  public String host;

  public String name;

  public String index;

  public String keywords;

  public String copyright;

  public String subTitle;

  public String description;

  public String otherFooter;

  @ConfigBinding("author.email")
  public String email;

  public final long startupTimeMillis = System.currentTimeMillis();

  @ConfigBinding(value = "article.feed.list.size", splice = false)
  public int articleFeedListSize = 10;

  @ConfigBinding("list.size")
  public int listSize = BlogConstant.DEFAULT_LIST_SIZE; // articles list size

  @ConfigBinding("max.list.size")
  public int maxPageSize = BlogConstant.DEFAULT_MAX_PAGE_SIZE;

}
