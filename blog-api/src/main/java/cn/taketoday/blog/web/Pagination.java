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

package cn.taketoday.blog.web;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.persistence.Page;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-23 15:58
 */
public class Pagination<T> implements ListableHttpResult<T> {

  @SuppressWarnings("rawtypes")
  private static final Pagination empty = new Pagination<>(0, 0, 0, 0, Collections.emptyList());

  /** amount of page */
  private final int pages;

  /** all row in database */
  private final int total;

  /** every page size */
  private final int size;

  /** current page */
  private final int current;

  // Json
  // --------------------------
  private final List<T> data;

  public Pagination(int pages, int total, int size, int current, List<T> data) {
    this.pages = pages;
    this.total = total;
    this.size = size;
    this.current = current;
    this.data = data;
  }

  public static <T> Pagination<T> ok(List<T> data, int totalRecord, Pageable pageable) {
    int pages = ((totalRecord - 1) / pageable.pageSize() + 1);
    return new Pagination<>(pages, totalRecord, pageable.pageSize(), pageable.pageNumber(), data);
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

  @Override
  public List<T> getData() {
    return data;
  }

  @Override
  public boolean equals(Object param) {
    if (this == param)
      return true;
    if (!(param instanceof Pagination<?> that))
      return false;
    return pages == that.pages && total == that.total
            && size == that.size && current == that.current
            && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pages, total, size, current, data);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("current", current)
            .append("pages", pages)
            .append("total", total)
            .append("size", size)
            .append("data", data)
            .toString();
  }

  // Static Factory Methods

  @SuppressWarnings("unchecked")
  public static <T> Pagination<T> empty() {
    return empty;
  }

  public static <T> Pagination<T> from(Page<T> page) {
    return new Pagination<>(page.getTotalPages(), page.getTotalRows().intValue(),
            page.getLimit(), page.getPageNumber(), page.getRows());
  }

}
