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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.console;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Label;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.service.LabelService;
import cn.taketoday.blog.util.BlogUtils;
import cn.taketoday.blog.util.DateFormatter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/30 22:29
 */
public class ArticleForm {

  @Nullable
  public String createAt;

  public String category;
  public String copyright;
  public Set<String> labels;

  public String cover;
  public String title;
  public PostStatus status;
  public String summary;
  public String content;
  public String markdown;
  public String password;

  public String uri;

  public static Article forArticle(ArticleForm form, LabelService labelService) {
    Set<Label> labels = getLabels(form, labelService);

    Article article = new Article();

    article.setLabels(labels);
    article.setTitle(form.title);
    article.setStatus(form.status);
    article.setContent(form.content);
    article.setSummary(form.summary);
    article.setCategory(form.category);
    article.setMarkdown(form.markdown);
    article.setCopyright(form.copyright);
    article.setPassword(StringUtils.hasText(form.password) ? form.password : null);
    article.setCover(StringUtils.hasText(form.cover)
                     ? form.cover
                     : BlogUtils.getFirstImagePath(form.content)
    );

    article.setUri(form.uri);
    if (StringUtils.hasText(form.createAt)) {
      article.setCreateAt(DateFormatter.parse(form.createAt));
    }
    return article;
  }

  @Nullable
  private static Set<Label> getLabels(ArticleForm from, LabelService labelService) {
    if (CollectionUtils.isNotEmpty(from.labels)) {
      var labels = new LinkedHashSet<Label>();
      for (String label : from.labels) {
        Label byName = labelService.getByName(label);
        if (byName == null) {
          byName = new Label().setName(label);
          labelService.save(byName);
        }
        labels.add(byName);
      }
      return labels;
    }
    return null;
  }

}
