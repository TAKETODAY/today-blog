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

package cn.taketoday.blog.web.interceptor;

import java.util.List;

import cn.taketoday.blog.event.ArticleUpdateEvent;
import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.ArticleItem;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.service.ArticleService;
import cn.taketoday.blog.web.ListableHttpResult;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MapCache;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/1/10 22:45
 */
public class ArticleFilterInterceptor extends SessionHandlerInterceptor implements ApplicationListener<ArticleUpdateEvent> {

  private final ArticleService articleService;

  private final MapCache<Long, Boolean, ArticleItem> passwordCache = new MapCache<>() {

    @Override
    protected Boolean createValue(Long id, ArticleItem item) {
      Article article = articleService.getById(id);
      return article != null && article.needPassword();
    }
  };

  public ArticleFilterInterceptor(SessionManager sessionManager, ArticleService articleService) {
    super(sessionManager);
    this.articleService = articleService;
  }

  @Override
  public void onApplicationEvent(ArticleUpdateEvent event) {
    long articleId = event.getArticleId();
    passwordCache.remove(articleId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterProcess(RequestContext context, Object handler, @Nullable Object result) {
    if (result instanceof ListableHttpResult<?> pagination
            && !Blogger.isPresent(getSession(context, false))) {

      // 过滤
      List<?> objects = pagination.getData();
      Object first = CollectionUtils.firstElement(objects);
      if (first instanceof ArticleItem) {
        for (ArticleItem item : (List<ArticleItem>) objects) {
          // 过滤有密码的
          if (passwordCache.get(item.id, item)) {
            item.cover = null;
            item.summary = "需要密码才能查看";
          }
        }
      }
    }
  }

}
