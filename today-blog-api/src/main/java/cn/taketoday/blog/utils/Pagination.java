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

package cn.taketoday.blog.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;

import cn.taketoday.blog.Pageable;
import lombok.NoArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-23 15:58
 */
@NoArgsConstructor
public class Pagination<T> implements ListableResult<T> {

  private static final long serialVersionUID = 1L;
  private static final Pagination empty = new Pagination<>();

  /** amount of page */
  private long num;
  /** all row in database */
  private long all;
  /** every page size */
  private int size;
  /** current page */
  private int current;

  // Json
  // --------------------------
  private List<T> data = Collections.emptyList();

  public Pagination<T> applyNum() {
    num = (all - 1) / size + 1;
    return this;
  }

  public long getNum() {
    return num;
  }

  public long getAll() {
    return all;
  }

  public int getSize() {
    return size;
  }

  public int getCurrent() {
    return current;
  }

  public Pagination<T> pageable(int all, Pageable pageable) {
    this.all = all;
    return pageable(pageable);
  }

  public Pagination<T> pageable(Pageable pageable) {
    this.size = pageable.getSize();
    this.current = pageable.getCurrent();
    applyNum();
    return this;
  }

  public Pagination<T> size(int size) {
    this.size = size;
    return this;
  }

  public Pagination<T> all(long all) {
    this.all = all;
    return this;
  }

  public Pagination<T> data(List<T> data) {
    this.data = data;
    return this;
  }

  public Pagination<T> num(long num) {
    this.num = num;
    return this;
  }

  public Pagination<T> current(int current) {
    this.current = current;
    return this;
  }

  // --------------------------------------------

  @JsonIgnore
  public boolean isSuccess() {
    return true;
  }

  @JsonIgnore
  public String getMessage() {
    return null;
  }

  @Override
  public List<T> getData() {
    return data;
  }

  //-------------------------

  public static <T> Pagination<T> create(List<T> data) {
    return new Pagination<T>().data(data);
  }

  public static <T> Pagination<T> ok() {
    return empty;
  }

  public static <T> Pagination<T> empty() {
    return empty;
  }

  public static <T> Pagination<T> ok(List<T> data) {
    return create(data);
  }

  public static <T> Pagination<T> ok(List<T> data, int all, Pageable pageable) {
    return create(data).pageable(all, pageable);
  }
}
