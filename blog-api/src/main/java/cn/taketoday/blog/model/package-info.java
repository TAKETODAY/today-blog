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

/**
 * 博客的数据模型，包括持久化实体和轻量查询模型。
 *
 * <p>本包使用 {@code model} 而非 {@code domain} 命名，表示通用的数据模型集合，
 * 不限定为承载业务规则和行为的领域模型，也不专指 HTTP 接口的请求或响应对象。
 *
 * <p>例如，{@link cn.taketoday.blog.model.Article Article} 表示完整文章实体，
 * {@link cn.taketoday.blog.model.ArticleItem ArticleItem} 表示不含正文的文章列表项。
 * 服务层可以按需直接查询并返回轻量模型，无须为了分层而先加载完整实体，
 * 再由控制器转换。若接口输出与查询模型存在实际差异，再在 Web 层定义专用的响应模型。
 */
package cn.taketoday.blog.model;
