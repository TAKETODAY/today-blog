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

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.1 2023/3/8 22:36
 */
@Data
public class ArticleItem {

  public long id;

  public int pv;
  public String uri;
  public String title;
  public String cover;
  public String summary;
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
}
