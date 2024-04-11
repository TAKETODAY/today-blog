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

package cn.taketoday.blog.model.form;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.blog.model.enums.OrderBy;
import cn.taketoday.blog.model.enums.PostStatus;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.core.Pair;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.persistence.ConditionStatement;
import cn.taketoday.persistence.EntityMetadata;
import cn.taketoday.persistence.EntityProperty;
import cn.taketoday.persistence.Order;
import cn.taketoday.persistence.sql.MutableOrderByClause;
import cn.taketoday.persistence.sql.OrderByClause;
import cn.taketoday.persistence.sql.Restriction;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY 2020/12/20 22:42
 */
@Getter
@Setter
public class ArticleConditionForm implements ConditionStatement {

  @Nullable
  private String q;

  @Nullable
  private String category;

  @Nullable
  private PostStatus status;

  @Nullable
  private Map<String, OrderBy> sort;

  @Nullable
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime[] createAt;

  @Nullable
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime[] updateAt;

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    if (StringUtils.hasText(q)) {
      restrictions.add(Restriction.plain(" (`title` like ? OR `content` like ? )"));
    }

    if (StringUtils.hasText(category)) {
      restrictions.add(Restriction.equal("category"));
    }

    if (status != null) {
      restrictions.add(Restriction.equal("status"));
    }

    if (createAt != null && createAt.length == 2) {
      restrictions.add(Restriction.plain("create_at between ? and ?"));
    }

    if (updateAt != null && updateAt.length == 2) {
      restrictions.add(Restriction.plain("update_at between ? and ?"));
    }

  }

  @Nullable
  @Override
  public OrderByClause getOrderByClause(EntityMetadata metadata) {
    if (sort != null) {
      List<Pair<String, Order>> list = sort.entrySet().stream()
              .map(entry -> {
                EntityProperty property = metadata.findProperty(entry.getKey());
                if (property != null) {
                  return Pair.of(property.columnName, entry.getValue().order);
                }
                return null;
              })
              .filter(Objects::nonNull)
              .toList();
      return new MutableOrderByClause(list);
    }
    return OrderByClause.plain("update_at DESC, create_at DESC");
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement smt) throws SQLException {
    int idx = 1;
    if (StringUtils.hasText(q)) {
      String string = '%' + q.trim() + '%';
      smt.setString(idx++, string);
      smt.setString(idx++, string);
    }

    if (StringUtils.hasText(category)) {
      smt.setString(idx++, category.trim());
    }

    if (status != null) {
      smt.setInt(idx++, status.getValue());
    }

    if (createAt != null && createAt.length == 2) {
      smt.setObject(idx++, createAt[0]);
      smt.setObject(idx++, createAt[1]);
    }

    if (updateAt != null && updateAt.length == 2) {
      smt.setObject(idx++, updateAt[0]);
      smt.setObject(idx, updateAt[1]);
    }

  }

  @Override
  public String getDescription() {
    return "Articles searching";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format("Articles searching with [{}]", this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final ArticleConditionForm that))
      return false;
    return status == that.status
            && Objects.equals(q, that.q)
            && Objects.equals(category, that.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(q, category, status);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("q", q)
            .append("category", category)
            .append("status", status)
            .append("sort", sort)
            .toString();
  }
}
