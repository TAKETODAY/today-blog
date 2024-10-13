import { http } from '@/utils';

// const DEFAULT_SESSION_TIMEOUT = 3 * 60 * 1000 // 3min
const DEFAULT_SESSION_TIMEOUT = 10 * 1000  // 10s

export default class UserService {

  login = (fromData) => {
    // console.log(fromData)
    return http.post('/api/auth', fromData)
  }

  logout() {
    return http.delete('/api/auth')
  }

  updateAvatar(data) {
    return http.patch('/api/auth?avatar', data)
  }

  updateInfo(data) {
    return http.patch('/api/auth', data)
  }

  updateEmailAndMobile(data) {
    return http.patch('/api/auth?email-mobile-phone', data)
  }

  updatePassword(data) {
    return http.patch(`/api/auth?password`, data)
  }

}
