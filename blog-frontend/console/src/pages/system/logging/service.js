import { http } from '@/utils'
import { request } from 'umi';

export async function queryLogging(params, sort) {
  const { pageSize, current, ...rest } = params
  params = { ...rest, ...sort, size: pageSize, page: current }
  return request('/api/console/logging', {
    method: 'GET',
    params
  }).then(res => {
    return {
      ...res,
      total: res.total,
      pageSize: res.size,
      current: res.current
    }
  })
}

export async function deleteById(id) {
  return http.delete(`/api/console/logging/${ id }`)
}
