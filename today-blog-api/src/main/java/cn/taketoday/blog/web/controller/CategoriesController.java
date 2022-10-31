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

package cn.taketoday.blog.web.controller;

import java.util.List;

import cn.taketoday.blog.ApplicationException;
import cn.taketoday.blog.aspect.Logger;
import cn.taketoday.blog.model.Category;
import cn.taketoday.blog.service.CategoryService;
import cn.taketoday.blog.utils.Json;
import cn.taketoday.blog.utils.StringUtils;
import cn.taketoday.blog.web.interceptor.AdminInterceptor;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.NotFoundException;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-20 19:07
 */
@Controller
@RequestMapping("/api/categories")
@RequiresBlogger
public class CategoriesController {

  private final CategoryService categoryService;

  public CategoriesController(final CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @POST
  @Logger(value = "创建分类", content = "name:[${category.name}]")
  public Json post(@RequestBody Category category) {
    validateCategory(category);

    if (categoryService.getCategory(category.getName()) != null) {
      return Json.failed("分类重复");
    }

    categoryService.save(category);
    return Json.ok("创建成功");
  }

  /**
   * Validate {@link Category}
   *
   * @param category {@link Category} instance
   */
  static void validateCategory(Category category) {
    if (StringUtils.isEmpty(category.getName())) {
      throw new ApplicationException("分类名不能为空");
    }
    if (category.getOrder() <= 0) {
      category.setOrder(Category.DEFAULT_ORDER);
    }
    if (StringUtils.isEmpty(category.getDescription())) {
      category.setDescription(category.getName());
    }
  }

  @PUT("/{name}")
  @Logger(value = "更新分类", content = "update name:[${name}]")
  public void put(@RequestBody Category category, @PathVariable String name) {
    Category oldCategory = categoryService.getCategory(name);
    NotFoundException.notNull(oldCategory, "要更新的分类不存在");

    if (oldCategory.equals(category)) {
      throw ApplicationException.failed("分类未更改");
    }
    categoryService.update(category, name);
  }

  @DELETE("/{name}")
  @Logger(value = "删除分类", content = "delete name:[${name}]")
  public void delete(@PathVariable String name) {
    NotFoundException.notNull(categoryService.getCategory(name), () -> "分类'" + name + "'不存在");
    categoryService.delete(name);
  }

  @GET
  @Interceptor(exclude = AdminInterceptor.class)
  public List<Category> all() {
    return categoryService.getAllCategories();
  }

  @GET("/{name}")
  @Interceptor(exclude = AdminInterceptor.class)
  public Category name(@PathVariable String name) {
    return categoryService.getCategory(name);
  }

}
