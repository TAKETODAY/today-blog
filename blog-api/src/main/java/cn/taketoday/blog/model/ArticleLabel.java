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

import cn.taketoday.persistence.Table;
import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/14 18:13
 */
@Data
@Table("article_label")
public class ArticleLabel {

  private Long labelId;

  private Long articleId;

  public ArticleLabel() { }

  public ArticleLabel(Long labelId, Long articleId) {
    this.labelId = labelId;
    this.articleId = articleId;
  }

  public static ArticleLabel forArticle(Long articleId) {
    return new ArticleLabel(null, articleId);
  }

  public static ArticleLabel of(Long labelId, Long articleId) {
    return new ArticleLabel(labelId, articleId);
  }
}
