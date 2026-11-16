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

package cn.taketoday.blog.web.console;

import java.util.List;

import cn.taketoday.blog.log.Logging;
import cn.taketoday.blog.model.Category;
import cn.taketoday.blog.service.CategoryService;
import cn.taketoday.blog.util.StringUtils;
import cn.taketoday.blog.error.ProblemException;
import cn.taketoday.blog.error.CommonProblems;
import cn.taketoday.blog.service.ContentProblems;
import cn.taketoday.blog.web.interceptor.RequiresBlogger;
import infra.http.HttpStatus;
import infra.stereotype.Controller;
import infra.web.annotation.DELETE;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PUT;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import lombok.RequiredArgsConstructor;

/**
 * 分类管理控制台处理器
 * <p>
 * 提供博客分类的增删改查接口，仅限博主访问。
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-09-20 19:07
 */
@Controller
@RequiresBlogger
@RequiredArgsConstructor
@RequestMapping("/api/console/categories")
class CategoriesConsoleHttpHandler {

  private final CategoryService categoryService;

  /**
   * 获取全部分类
   */
  @GET
  public List<Category> listCategories() {
    return categoryService.getOrderedCategories();
  }

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
      throw new ProblemException(ContentProblems.CATEGORY_REQUIRED);
    }
    if (category.getOrder() == null) {
      category.setOrder(Category.DEFAULT_ORDER);
    }
    if (StringUtils.isBlank(category.getDescription())) {
      category.setDescription(category.getName());
    }

    if (categoryService.findCategory(category.getName()) != null) {
      throw new ProblemException(ContentProblems.CATEGORY_EXISTS);
    }

    categoryService.persist(category);
  }

  /**
   * 更新分类
   */
  @PUT("/{name}")
  @Logging(title = "更新分类", content = "name:[#{#name}]")
  public void update(@RequestBody Category category, @PathVariable String name) {
    Category oldCategory = categoryService.findCategory(name);
    ProblemException.notNull(oldCategory, ContentProblems.CATEGORY_NOT_FOUND);

    if (oldCategory.equals(category)) {
      throw new ProblemException(CommonProblems.NO_CHANGES);
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
    ProblemException.notNull(categoryService.findCategory(name), ContentProblems.CATEGORY_NOT_FOUND);
    categoryService.deleteById(name);
  }

}
