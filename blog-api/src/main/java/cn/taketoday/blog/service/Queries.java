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

import java.sql.PreparedStatement;
import java.util.function.Consumer;

import cn.taketoday.persistence.EntityMetadata;
import cn.taketoday.persistence.QueryStatement;
import cn.taketoday.persistence.SimpleSelectQueryStatement;
import cn.taketoday.persistence.sql.SimpleSelect;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.2 2024/10/12 22:15
 */
public class Queries {

  public static QueryStatement forSelect(Consumer<SimpleSelect> consumer) {
    return new SimpleSelectQueryStatement() {

      @Override
      protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
        consumer.accept(select);
      }

      @Override
      public void setParameter(EntityMetadata metadata, PreparedStatement statement) {

      }
    };
  }

}
