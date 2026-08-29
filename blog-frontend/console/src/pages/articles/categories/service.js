import { http } from "@/utils";
import { request } from 'umi';

export async function queryCategories(params) {
  const { pageSize, current, ...rest } = params
  return request(`/api/console/categories?size=${ pageSize }&current=${ current }`, { method: "GET", params: rest })
      .then(data => ({ data }))
}

export async function create(data) {
  return http.post('/api/console/categories', data)
}

export async function update(oldName, category) {
  return http.put(`/api/console/categories/${ oldName }`, category)
}

export async function deleteCategory(category) {
  return http.delete(`/api/console/categories/${ category.name }`)
}

