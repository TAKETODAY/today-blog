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

package cn.taketoday.blog.service;

import java.util.List;

import cn.taketoday.blog.Pageable;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.blog.repository.UserRepository;
import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.stereotype.Service;
import cn.taketoday.web.InternalServerException;
import cn.taketoday.web.NotFoundException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-12-06 22:09
 */
@Service
@CacheConfig(cacheNames = "users")
public class UserService {
  public final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public List<User> get(Pageable pageable) {
    return get(pageable.current(), pageable.size());
  }

  public List<User> getByStatus(UserStatus status, Pageable pageable) {
    return getByStatus(status, pageable.current(), pageable.size());
  }

  public String getAvatar(String email) {
    User findByEmail = getByEmail(email);
    if (findByEmail == null) {
      throw new NotFoundException("用户'" + email + "'不存在");
    }
    return findByEmail.getAvatar();
  }

  @CacheEvict(allEntries = true)
  public void update(User user) {
    try {
      repository.update(user);
    }
    catch (Exception e) {
      throw InternalServerException.failed("更新失败", e);
    }
  }

  @Cacheable(key = "'email_'+#email")
  public User getByEmail(String email) {
    return repository.findByEmail(email);
  }

  public void register(User user) {
    repository.save(user);
  }

  @Cacheable(key = "'ById_'+#id")
  public User getById(long id) {
    return repository.findById(id);
  }

  public int count() {
    return repository.getTotalRecord();
  }

  public int countByStatus(UserStatus status) {
    return repository.getRecord(status);
  }

  public List<User> getByStatus(UserStatus status, int pageNow, int pageSize) {
    return repository.findByStatus(status, (pageNow - 1) * pageSize, pageSize);
  }

  public List<User> get(int pageNow, int pageSize) {
    return repository.find((pageNow - 1) * pageSize, pageSize);
  }

  @CacheEvict(allEntries = true)
  public void updateStatusById(UserStatus status, long id) {
    repository.updateStatus(status, id);
  }

  @CacheEvict(allEntries = true)
  public void deleteById(long id) {
    repository.deleteById(id);
  }

}
