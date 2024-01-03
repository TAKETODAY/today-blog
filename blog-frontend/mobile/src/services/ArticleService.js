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

import { getCacheable, http } from 'src/utils'

export default class ArticleService {

  getAllCategories() {
    return getCacheable("/api/categories")
  }

  getAllTags() {
    return getCacheable("/api/tags")
  }

  getByTagName(name, page = 1) {
    return getCacheable(`/api/articles?tag=${ name }&page=${ page }`)
  }

  getByCategoryName(name, page = 1) {
    return getCacheable(`/api/articles?category=${ name }&page=${ page }`)
  }

  getById(articleId, key) {
    return http.get(`/api/articles/${ articleId }${ key ? '?key=' + key : '' }`)
  }

  getIndexArticles(current) {
    return getCacheable(`/api/articles?page=${ current }`)
  }

  updatePV(id) {
    return http.patch(`/api/articles/${ id }/pv`)
  }

  search(search, page = 1) {
    return http.get(`/api/articles?q=${ search }&page=${ page }`)
  }

}

