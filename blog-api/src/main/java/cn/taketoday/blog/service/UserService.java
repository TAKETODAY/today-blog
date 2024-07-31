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

package cn.taketoday.blog.service;

import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.EntityManager;
import cn.taketoday.persistence.EntityRef;
import cn.taketoday.persistence.Id;
import cn.taketoday.stereotype.Service;
import cn.taketoday.web.ResponseStatusException;

import static cn.taketoday.persistence.QueryCondition.isEqualsTo;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-12-06 22:09
 */
@Service
@CacheConfig(cacheNames = "users")
public class UserService {

  private final EntityManager entityManager;

  public UserService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public String getAvatar(String email) {
    User findByEmail = getByEmail(email);
    if (findByEmail == null) {
      throw ErrorMessageException.failed("用户 '%s' 不存在".formatted(email));
    }
    return findByEmail.getAvatar();
  }

  @CacheEvict(allEntries = true)
  public void updateById(User user) {
    try {
      entityManager.updateById(user);
    }
    catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "更新失败", e);
    }
  }

  @Nullable
  @Cacheable(key = "'email_'+#email")
  public User getByEmail(String email) {
    return entityManager.findUnique(User.class, isEqualsTo("email", email));
  }

  public void register(User user) {
    entityManager.persist(user);
  }

  @Nullable
  @Cacheable(key = "'ById_'+#id")
  public User getById(long id) {
    return entityManager.findById(User.class, id);
  }

  public int count() {
    return entityManager.count(User.class).intValue();
  }

  @CacheEvict(allEntries = true)
  public void updateStatusById(UserStatus status, long id) {
    entityManager.updateById(new UserStatusUpdate(id, status));
  }

  @EntityRef(User.class)
  static class UserStatusUpdate {

    @Id
    public final Long id;

    public final UserStatus status;

    UserStatusUpdate(Long id, UserStatus status) {
      this.id = id;
      this.status = status;
    }
  }

  @CacheEvict(allEntries = true)
  public void deleteById(long id) {
    entityManager.delete(User.class, id);
  }

}
