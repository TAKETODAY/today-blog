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

package cn.taketoday.blog;

import java.util.Collections;
import java.util.List;

import lombok.NoArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-23 15:58
 */
@NoArgsConstructor
public class Pagination<T> implements ListableHttpResult<T> {

  @SuppressWarnings("rawtypes")
  private static final Pagination empty = new Pagination<>();

  /** amount of page */
  private int pages;

  /** all row in database */
  private int total;

  /** every page size */
  private int size;

  /** current page */
  private int current;

  // Json
  // --------------------------
  private List<T> data = Collections.emptyList();

  public Pagination<T> applyNum() {
    pages = (total - 1) / size + 1;
    return this;
  }

  public int getPages() {
    return pages;
  }

  public int getTotal() {
    return total;
  }

  public int getSize() {
    return size;
  }

  public int getCurrent() {
    return current;
  }

  public Pagination<T> pageable(int total, Pageable pageable) {
    this.total = total;
    return pageable(pageable);
  }

  public Pagination<T> pageable(Pageable pageable) {
    this.size = pageable.size();
    this.current = pageable.current();
    applyNum();
    return this;
  }

  public Pagination<T> size(int size) {
    this.size = size;
    return this;
  }

  public Pagination<T> total(int all) {
    this.total = all;
    return this;
  }

  public Pagination<T> data(List<T> data) {
    this.data = data;
    return this;
  }

  public Pagination<T> pageCount(int num) {
    this.pages = num;
    return this;
  }

  public Pagination<T> current(int current) {
    this.current = current;
    return this;
  }

  @Override
  public List<T> getData() {
    return data;
  }

  // Static Factory Methods

  public static <T> Pagination<T> empty() {
    return empty;
  }

  public static <T> Pagination<T> ok(List<T> data) {
    return new Pagination<T>().data(data);
  }

  public static <T> Pagination<T> ok(List<T> data, int total, Pageable pageable) {
    return ok(data).pageable(total, pageable);
  }
}
