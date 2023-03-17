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

import { http } from "@/utils";
import { request } from 'umi';

export async function queryComments(params) {
  const { pageSize, current, ...rest } = params
  return request(`/api/comments?size=${pageSize}&page=${current}`, { method: "GET", params: rest })
    .then(res => {
      return {
        ...res,
        total: res.total,
        pageSize: res.size,
        current: res.current
      }
    })
}

export async function create(data) {
  return http.post('/api/comments', data)
}

export async function update(id, comment) {
  return http.put(`/api/comments/${id}`, comment)
}

export async function deleteComment(comment) {
  return http.delete(`/api/comments/${comment.id}`)
}

