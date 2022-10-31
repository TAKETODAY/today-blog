import { http } from '@/utils'
import { request } from 'umi';

export async function queryLogging(params, sort) {
  const { pageSize, current, ...rest } = params
  params = { ...rest, ...sort, size: pageSize, page: current }
  return request('/api/logging', {
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

export async function deleteById(id) {
  return http.delete(`/api/logging/${ id }`)
}
