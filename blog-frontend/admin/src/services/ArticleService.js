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

import { getCacheable, http } from '@/utils'

export default new class {

  create(article) {
    return http.post('/api/articles', article)
  }

  update(article) {
    return http.put('/api/articles/' + article.id, article)
  }

  updateById(id, article) {
    delete article['id']
    delete article['updateAt']
    const createAt = moment(article.createAt).format('yyyy-MM-DD HH:mm:ss')
    return http.put('/api/articles/' + id, { ...article, createAt })
  }

  getAllCategories() {
    return getCacheable('/api/categories')
  }

  getById(id) {
    return http.get(`/api/articles/${id}`)
  }

  getAllLabels() {
    return getCacheable('/api/tags')
  }

}()
