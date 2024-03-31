/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { extractData, http } from '@/utils'

export async function queryArticles(params, sort) {
  const { pageSize, current, ...rest } = params
  params = { ...rest, sort, size: pageSize, page: current }
  return http.get('/api/console/articles', { params }).then(extractData).then(data => {
    return {
      ...data,
      success: true,
      pageSize: data.size,
      current: data.current
    }
  })
}

export async function getCategories() {
  return http.get('/api/categories')
}

export async function toggleArticleStatus(id, status) {
  return http.patch(`/api/console/articles/${id}?status=${status}`)
}

export async function deleteArticle(article) {
  return http.delete(`/api/console/articles/${article.id}`)
}
