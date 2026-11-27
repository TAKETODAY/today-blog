import { http } from "@/utils";

/** 获取当前用户，204 表示未登录。 */
export async function currentUser() {
  return http.get('/api/auth').then(response => ({
    ...response,
    data: response.status === 204 ? undefined : response.data,
  }))
}

/** 此处后端没有提供注释 GET /api/notices */
export async function getNotices() {
  return http.get('/api/notices')
}
