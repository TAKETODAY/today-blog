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

package cn.taketoday.blog.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-08 08:22
 */
@Getter
@Setter
public class Sitemap implements Serializable {
  static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Serial
  private static final long serialVersionUID = 1L;

  //	always hourly daily weekly monthly yearly never

  private final LinkedList<SiteURL> urls = new LinkedList<>();

  public void addArticle(Article article) {
    addUrl(newURL(article));
  }

  public void addUrl(SiteURL url) {
    urls.add(url);
  }

  public static SiteURL newURL(Article article) {
    LocalDateTime updateAt = article.getUpdateAt();
    if (updateAt == null) {
      updateAt = article.getCreateAt();
      if (updateAt == null) {
        updateAt = LocalDateTime.now();
      }
    }

    String format = formatter.format(updateAt);
    return newURL(0.9f, "/articles/" + article.getUri(), format, "daily");
  }

  public static SiteURL newURL(float priority, String location, String lastModify, String changeFreq) {
    return new SiteURL(location, priority, lastModify, changeFreq);
  }

  @Getter
  public static final class SiteURL implements Serializable {

    private final String loc; // location

    private final float priority; // 优先级

    @Nullable
    private final String lastModify;// 最后更改

    private final String changeFreq;// 更新频率

    public SiteURL(String loc, float priority, @Nullable String lastModify, String changeFreq) {
      this.loc = loc;
      this.priority = priority;
      this.lastModify = lastModify;
      this.changeFreq = changeFreq;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Sitemap sitemap))
      return false;
    return Objects.equals(urls, sitemap.urls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(urls);
  }
}
