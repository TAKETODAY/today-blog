import { convertEmpty, http } from "@/utils";

export async function getAttachment(params) {
  const { size, current, ...rest } = params
  convertEmpty(rest)
  return http.get(`/api/attachments?size=${size}&page=${current}`, { params: rest })
}
