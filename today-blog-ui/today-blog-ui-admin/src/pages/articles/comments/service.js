import { http } from "@/utils";
import { request } from 'umi';

export async function queryComments(params) {
  const { pageSize, current, ...rest } = params
  return request(`/api/comments?size=${ pageSize }&page=${ current }`, { method: "GET", params: rest })
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
  return http.post('/api/comments', data)
}

export async function update(id, comment) {
  return http.put(`/api/comments/${ id }`, comment)
}

export async function deleteComment(comment) {
  return http.delete(`/api/comments/${ comment.id }`)
}

