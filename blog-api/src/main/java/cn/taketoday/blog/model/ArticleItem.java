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

package cn.taketoday.blog.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.persistence.EntityRef;
import cn.taketoday.persistence.Transient;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/8 22:36
 */
@Data
@EntityRef(Article.class)
public class ArticleItem {

  public long id;

  public int pv;

  public String uri;

  public String title;

  public String cover;

  public String summary;

  @Transient
  public List<String> tags;

  public LocalDateTime createAt;

  public ArticleItem() { }

  public ArticleItem(Article article) {
    this.id = article.getId();
    this.pv = article.getPv();
    this.uri = article.getUri();
    this.title = article.getTitle();
    this.cover = article.getCover();
    this.summary = article.getSummary();
    this.createAt = article.getCreateAt();
    this.tags = article.getLabels().stream().map(Label::getName).toList();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ArticleItem that))
      return false;
    return id == that.id && pv == that.pv
            && Objects.equals(uri, that.uri)
            && Objects.equals(title, that.title)
            && Objects.equals(cover, that.cover)
            && Objects.equals(summary, that.summary)
            && Objects.equals(createAt, that.createAt)
            && Objects.equals(tags, that.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, pv, uri, title, cover, summary, tags, createAt);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("cover", cover)
            .append("title", title)
            .append("pv", pv)
            .append("summary", StringUtils.truncate(summary, 10))
            .append("createAt", createAt)
            .append("uri", uri)
            .toString();
  }
}
