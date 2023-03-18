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
import java.util.Objects;
import java.util.Set;

import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.jdbc.persistence.Id;
import cn.taketoday.jdbc.persistence.Table;
import cn.taketoday.jdbc.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * 文章实体
 *
 * @since 2017 10 11--18:07
 */
@Setter
@Getter
@Table("article")
public class Article implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  private Long id;

  private String cover;

  private String title;

  private int pv;

  private PostStatus status;

  private String summary;

  /** html content **/
  private String content;

  /** markdown content */
  private String markdown;

  /** 需要输入密码才能访问该页面 */
  private String password;

  private String uri;

  /** category name */
  private String category;
  private String copyright;

  @Transient
  private Set<Label> labels;

  private LocalDateTime createAt;
  private LocalDateTime updateAt;

  public boolean needPassword() {
    return password != null;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("id", id)
            .append("cover", cover)
            .append("title", title)
            .append("pv", pv)
            .append("status", status)
            .append("summary", summary)
            .append("content", content)
            .append("markdown", markdown)
            .append("password", password)
            .append("createAt", createAt)
            .append("updateAt", updateAt)
            .append("uri", uri)
            .append("category", category)
            .append("copyright", copyright)
            .append("labels", labels)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Article article))
      return false;
    return pv == article.pv
            && status == article.status
            && Objects.equals(id, article.id)
            && Objects.equals(uri, article.uri)
            && Objects.equals(cover, article.cover)
            && Objects.equals(title, article.title)
            && Objects.equals(summary, article.summary)
            && Objects.equals(content, article.content)
            && Objects.equals(markdown, article.markdown)
            && Objects.equals(password, article.password)
            && Objects.equals(createAt, article.createAt)
            && Objects.equals(updateAt, article.updateAt)
            && Objects.equals(category, article.category)
            && Objects.equals(copyright, article.copyright)
            && Objects.equals(labels, article.labels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, cover, title, pv, status, summary, content, markdown,
            password, createAt, updateAt, uri, category, copyright, labels);
  }
}
