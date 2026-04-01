/*
 * Copyright 2017 - 2025 the original author or authors.
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

import { http } from '@/utils'
import { request } from "umi";

export async function queryUsers(params) {
  const { pageSize, current, ...rest } = params
  return request(`/api/console/users?size=${ pageSize }&page=${ current }`, { params: rest })
      .then(res => {
        return {
          ...res,
          total: res.all,
          pageSize: res.size,
          current: res.current
        }
      })
}

export async function create(data) {
  return http.post('/api/console/users', data)
}

export async function update(users) {
  return http.put(`/api/console/users/${ users.id }`, users)
}

export async function deleteUser(book) {
  return http.delete(`/api/console/users/${ book.id }`)
}

export async function toggleStatus(id, status) {
  return http.put(`/api/console/users/${ id }/status/${ status }`)
}
