import { http } from "@/utils";
import { request } from 'umi';

export async function queryCategories(params) {
  const { pageSize, current, ...rest } = params
  return request(`/api/categories?size=${ pageSize }&current=${ current }`, { method: "GET", params: rest })
      .then(data => ({ data }))
}

export async function create(data) {
  return http.post('/api/categories', data)
}

export async function update(oldName, category) {
  return http.put(`/api/categories/${ oldName }`, category)
}

export async function deleteCategory(category) {
  return http.delete(`/api/categories/${ category.name }`)
}

