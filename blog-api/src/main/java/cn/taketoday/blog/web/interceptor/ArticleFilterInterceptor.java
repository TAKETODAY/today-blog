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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.blog.web.interceptor;

import java.util.List;

import cn.taketoday.blog.model.Article;
import cn.taketoday.blog.model.Blogger;
import cn.taketoday.blog.web.Pagination;
import cn.taketoday.session.SessionHandlerInterceptor;
import cn.taketoday.session.SessionManager;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/1/10 22:45
 */
public class ArticleFilterInterceptor extends SessionHandlerInterceptor {

  public ArticleFilterInterceptor(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void afterProcess(RequestContext context, Object handler, Object result) {
    if (result instanceof Pagination<?> pagination
            && !Blogger.isPresent(getSession(context, false))) {

      // 过滤
      List<?> objects = pagination.getData();
      Object first = CollectionUtils.firstElement(objects);
      if (first instanceof Article) {
        for (Article article : (List<Article>) objects) {
          // 过滤有密码的
          if (article.needPassword()) {
            article.setCover(null);
            article.setStatus(null);
            article.setLabels(null);
            article.setPassword(null);

            article.setSummary("需要密码查看");
            article.setContent("需要密码查看");
            article.setMarkdown("需要密码查看");
            article.setCategory("需要密码查看");
          }
        }
      }
    }
  }

}
