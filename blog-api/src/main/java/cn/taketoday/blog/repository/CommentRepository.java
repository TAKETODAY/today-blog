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

package cn.taketoday.blog.repository;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import cn.taketoday.blog.model.Comment;
import cn.taketoday.blog.model.User;
import cn.taketoday.blog.model.enums.CommentStatus;
import cn.taketoday.stereotype.Repository;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-10 19:42
 */
@Repository
public interface CommentRepository extends DefaultRepository<Comment, Long> {

  @ResultMap("BaseResultMap")
  @Select("SELECT * FROM comment ORDER BY id DESC LIMIT 0, 5")
  List<Comment> findLatest();

  List<Comment> findByStatus(@Param("status") CommentStatus status, //
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  int getRecord(@Param("status") CommentStatus status);

  @Update("UPDATE comment set status = #{status} WHERE id = #{id}")
  void updateStatus(@Param("status") CommentStatus status, @Param("id") long id);

  List<Comment> findByUser(@Param("user") User user, //
          @Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  int getRecordByUser(@Param("user") User userInfo);

  @Update("UPDATE comment set sendMail = 1")
  void closeAllNotification();

  @Update("UPDATE comment set `sendMail` = 1 where `id`=#{id}")
  void checked(long id);

}
