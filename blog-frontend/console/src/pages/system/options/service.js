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

import { extractData, http } from '@/utils'

export async function queryOptions() {
  return http.get('/api/console/options')
}

export async function query(params, sort) {
  const { pageSize, current, ...rest } = params
  params = { ...rest, sort, size: pageSize, page: current }
  return http.get('/api/console/options', { params }).then(extractData).then(data => {
    return {
      ...data,
      success: true,
      pageSize: data.size,
      current: data.current
    }
  })
}


export async function updateOption(option) {
  return http.put('/api/console/options', option)
}

export async function doUpdateOptions(options) {
  console.log(options)
  return http.put('/api/options', options)
}
