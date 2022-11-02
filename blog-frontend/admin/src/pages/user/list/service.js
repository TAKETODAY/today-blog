import { http } from '@/utils'
import { request } from "umi";

export async function queryUsers(params) {
  const { pageSize, current, ...rest } = params
  return request(`/api/users?size=${ pageSize }&page=${ current }`, { params: rest })
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
  return http.post('/api/users', users)
}

export async function update(users) {
  return http.put(`/api/users/${ users.id }`, users)
}

export async function deleteUser(book) {
  return http.delete(`/api/users/${ book.id }`)
}

export async function toggleStatus(id, status) {
  return http.put(`/api/users/${ id }/status/${ status }`)
}
