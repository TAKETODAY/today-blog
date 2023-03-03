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

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.blog.model.enums.CommentStatus;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-03-17 22:22
 */
public class CommentStatusHandler implements TypeHandler<CommentStatus> {

  @Override
  public void setParameter(PreparedStatement ps, int i, CommentStatus parameter, JdbcType jdbcType) throws SQLException {
    ps.setInt(i, parameter.getValue());
  }

  @Override
  public CommentStatus getResult(ResultSet rs, String columnName) throws SQLException {
    return CommentStatus.valueOf(rs.getInt(columnName));
  }

  @Override
  public CommentStatus getResult(ResultSet rs, int columnIndex) throws SQLException {
    return CommentStatus.valueOf(rs.getInt(columnIndex));
  }

  @Override
  public CommentStatus getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return CommentStatus.valueOf(cs.getInt(columnIndex));
  }

}
