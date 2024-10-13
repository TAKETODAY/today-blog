// @ts-ignore
/* eslint-disable */
import { http } from "@/utils";

// Record<string, any>

/** 登录接口 POST /api/auth */
export async function outLogin() {
  return http.delete('/api/auth')
}

/** 登录接口 POST /api/login/account */
export async function login(body: API.LoginParams, options?: { [key: string]: any }) {
  return http.post('/api/auth', body, options)
}
