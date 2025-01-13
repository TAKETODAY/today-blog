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

package cn.taketoday.blog.util;

import cn.taketoday.blog.BlogConstant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-08 16:43
 */
public abstract class StringUtils extends infra.util.StringUtils {

  /**
   * Delete the input string text html
   *
   * @param htmlStr input text
   * @return none html text
   */
  public static String delHtml(String htmlStr) {
    String blank = BlogConstant.BLANK;
    if (htmlStr == null) {
      return blank;
    }
    return htmlStr
            .replaceAll(" ", blank)
            .replaceAll(BlogConstant.HTML_TAG, blank)
            .replaceAll("&nbsp;", blank);
  }

  public static String getRandomImageName(String name) {
    if (name == null) {
      name = BlogConstant.BLANK;
    }
    if (name.lastIndexOf('.') <= -1) {
      name = name.concat(".png");
    }
    return StringUtils.getUUIDString().split("-")[0].concat(name);
  }

}
