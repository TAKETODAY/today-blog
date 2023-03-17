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

import { http } from '@/utils'

export async function queryArticles(params, sort) {
  const { pageSize, current, ...rest } = params
  params = { ...rest, ...sort, size: pageSize, page: current }
  return http.get('/api/articles/admin', {
    method: 'GET',
    params
  }).then(res => {
    return {
      ...res,
      total: res.all,
      pageSize: res.size,
      current: res.current
    }
  })
}

export async function getCategories() {
  return http.get('/api/categories')
}

export async function toggleArticleStatus(id, status) {
  return http.put(`/api/articles/${id}/status/${status}`)
}


export async function create(data) {
  return http.post('/api/books', data)
}

export async function update(book) {
  return http.put(`/api/books/${book.id}`, book)
}

export async function deleteArticle(article) {
  return http.delete(`/api/articles/${article.id}`)
}

export async function toggleAlreadySold(id) {
  return http.put(`/api/books/${id}/toggle-already-sold`)
}
