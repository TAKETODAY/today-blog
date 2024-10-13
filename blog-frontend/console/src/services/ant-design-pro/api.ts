import { http } from "@/utils";

/** 获取当前的用户 GET /api/currentUser */
export async function currentUser() {
  return http.get('/api/auth')
}

/** 此处后端没有提供注释 GET /api/notices */
export async function getNotices() {
  return http.get('/api/notices')
}
