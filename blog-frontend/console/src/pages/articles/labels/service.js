import { http } from "@/utils";
import { request } from 'umi';

export async function queryTags() {
  return request('/api/console/tags').then(data => ({ data }))
}

export async function create(data) {
  return http.post('/api/console/tags/' + data)
}

export async function update(tag) {
  return http.put(`/api/console/tags/${ tag.id }?name=${ tag.name }`)
}

export async function deleteLabel(tag) {
  return http.delete(`/api/console/tags/${ tag.id }`)
}

