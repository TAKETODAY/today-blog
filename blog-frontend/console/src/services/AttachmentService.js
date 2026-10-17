import { http } from '@/utils'

export default new class {

  getAllAttachments(current = 1, size = 10) {
    return http.get(`/api/console/attachments?size=${ size }&page=${ current }`)
  }

}()
