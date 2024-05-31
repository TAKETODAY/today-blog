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

package cn.taketoday.blog.service;

import cn.taketoday.blog.model.Blogger;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.persistence.EntityManager;
import cn.taketoday.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-07 21:57
 */
@Service
@RequiredArgsConstructor
public class BloggerService {

  private volatile Blogger blogger;

  private final EntityManager entityManager;

  private final RepositoryManager repository;

  public Blogger fetchBlogger() {
    try (Query query = repository.createQuery("  SELECT * FROM blogger LIMIT 1")) {
      Blogger blogger = query.iterate(Blogger.class).unique();
      setBlogger(blogger);
      return blogger;
    }
  }

  public void updatePassword(String password) {
    Blogger blogger = new Blogger();
    blogger.setPasswd(password);
    blogger.setId(getBlogger().getId());
    entityManager.updateById(blogger);
    getBlogger().setPasswd(password);
  }

  public void setBlogger(Blogger blogger) {
    this.blogger = blogger;
  }

  public Blogger getBlogger() {
    Blogger blogger = this.blogger;
    if (blogger == null) {
      synchronized(this) {
        blogger = this.blogger;
        if (blogger == null) {
          blogger = fetchBlogger();
          setBlogger(blogger);
        }
      }
    }
    return blogger;
  }

}
