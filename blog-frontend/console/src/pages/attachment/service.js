import { http } from '@/utils'

export async function update(attach) {
  return http.put(`/api/attachments/${ attach.id }`, attach)
}

export async function deleteAttach(attach) {
  return http.delete(`/api/attachments/${ attach.id }`)
}
