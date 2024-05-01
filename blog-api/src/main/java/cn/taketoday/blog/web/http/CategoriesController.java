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

package cn.taketoday.blog.web.http;

import java.util.List;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Category;
import cn.taketoday.blog.service.CategoryService;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.web.ErrorMessageException;
import cn.taketoday.blog.web.interceptor.NotRequiresBlogger;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.DELETE;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PUT;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseStatus;
import lombok.RequiredArgsConstructor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-20 19:07
 */
@Controller
@RequiresBlogger
@RequiredArgsConstructor
@RequestMapping("/api/categories")
class CategoriesController {

  private final CategoryService categoryService;

  /**
   * 创建分类
   *
   * @param category 分类
   */
  @POST
  @ResponseStatus(HttpStatus.CREATED)
  @Logging(title = "创建分类", content = "name:[#{#category.name}]")
  public void create(@RequestBody Category category) {
    if (StringUtils.isBlank(category.getName())) {
      throw ErrorMessageException.failed("分类名不能为空");
    }
    if (category.getOrder() <= 0) {
      category.setOrder(Category.DEFAULT_ORDER);
    }
    if (StringUtils.isBlank(category.getDescription())) {
      category.setDescription(category.getName());
    }
    if (categoryService.getCategory(category.getName()) != null) {
      throw ErrorMessageException.failed("分类重复");
    }

    categoryService.save(category);
  }

  /**
   * 更新分类
   */
  @PUT("/{name}")
  @Logging(title = "更新分类", content = "name:[#{#name}]")
  public void update(@RequestBody Category category, @PathVariable String name) {
    Category oldCategory = categoryService.getCategory(name);
    ErrorMessageException.notNull(oldCategory, "要更新的分类不存在");

    if (oldCategory.equals(category)) {
      throw ErrorMessageException.failed("分类未更改");
    }
    categoryService.updateById(category, name);
  }

  /**
   * 删除 分类
   *
   * @param name 分类名称
   */
  @DELETE("/{name}")
  @Logging(title = "删除分类", content = "delete name:[#{#name}]")
  public void delete(@PathVariable String name) {
    ErrorMessageException.notNull(categoryService.getCategory(name), () -> "分类'" + name + "'不存在");
    categoryService.deleteById(name);
  }

  /**
   * 获取全部分类
   */
  @GET
  @NotRequiresBlogger
  public List<Category> listAll() {
    return categoryService.getAllCategories();
  }

  /**
   * 获取文章分类
   *
   * @param name 分类
   * @return 文章分类
   */
  @GET("/{name}")
  @NotRequiresBlogger
  public Category name(@PathVariable String name) {
    Category category = categoryService.getCategory(name);
    ErrorMessageException.notNull(category, "分类不存在");
    return category;
  }

}
