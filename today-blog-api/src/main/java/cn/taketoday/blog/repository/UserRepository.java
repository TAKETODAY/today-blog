/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.UserStatus;
import cn.taketoday.stereotype.Repository;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-04-08 10:43
 */
@Repository
public interface UserRepository extends DefaultRepository<User, Long> {

  User findByEmail(String email);

  List<User> findByStatus(@Param("status") UserStatus status, //
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  List<User> find(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  int getRecord(@Param("status") UserStatus status);

  @Update("UPDATE user set status = #{status.code} WHERE id = #{id}")
  void updateStatus(@Param("status") UserStatus status, @Param("id") long id);
}
