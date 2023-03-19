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

import { getCacheable, http } from '../utils';

export default class ArticleService {

  getById(articleId, key) {
    return http.get(`/api/articles/${articleId}${key ? '?key=' + key : ''}`)
  }

  popularArticles() {
    return getCacheable("/api/articles?most-popular")
  }

  search(query) {
    return http.get(`/api/articles?q=${query}`)
  }

  searchPageable(query, page = 1, size = 10) {
    return http.get(`/api/articles?q=${query}&page=${page}&size=${size}`)
  }

  updatePageView(articleId) {
    return http.patch(`/api/articles/${articleId}/pv`)
  }

  fetchHomeArticles(page = 1, size = 10) {
    return http.get(`/api/articles?page=${page}&size=${size}`)
  }

  getArticlesByTag(tag, page = 1, size = 10) {
    return http.get(`/api/articles?tag=${tag}&page=${page}&size=${size}`)
  }

  getArticlesByCategory(type, page = 1, size = 10) {
    return getCacheable(`/api/articles?category=${type}&page=${page}&size=${size}`)
  }

}

