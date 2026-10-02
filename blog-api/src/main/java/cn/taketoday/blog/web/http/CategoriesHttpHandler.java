/*
 * Copyright 2017 - 2026 the original author or authors.
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

package cn.taketoday.blog.web.http;

import java.util.List;

import cn.taketoday.blog.model.Category;
import cn.taketoday.blog.service.CategoryService;
import cn.taketoday.blog.web.ErrorMessageException;
import infra.stereotype.Controller;
import infra.web.annotation.GET;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;

/**
 * 分类 HTTP 处理器
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-20 19:07
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/categories")
class CategoriesHttpHandler {

  private final CategoryService categoryService;

  /**
   * 获取全部分类
   */
  @GET
  public List<Category> listAll() {
    return categoryService.getOrderedCategories();
  }

  /**
   * 获取文章分类
   *
   * @param name 分类
   * @return 文章分类
   */
  @GET("/{name}")
  public Category name(@PathVariable String name) {
    Category category = categoryService.findCategory(name);
    ErrorMessageException.notNull(category, "分类不存在");
    return category;
  }

}
