import { currentTimeMillis, http } from 'src/utils';
import store from 'src/store'

// const DEFAULT_SESSION_TIMEOUT = 3 * 60 * 1000 // 3min
const DEFAULT_SESSION_TIMEOUT = 10 * 1000  // 10s

export default class UserService {

  async getSession() {
    let session = store.getters["user/session"];
    if (session == null || currentTimeMillis() - session.expired >= DEFAULT_SESSION_TIMEOUT) {
      session = await http.get('/api/auth').then((res) => {
        res.data['expired'] = currentTimeMillis()
        return res.data
      }).catch((res) => null)
      store.commit('user/setSession', session)
    }
    return session
  }

  async isLoggedIn() {
    const session = await this.getSession()
    console.log('isLoggedIn', session !== null)
    return session !== null
  }

  login = (fromData) => {
    // console.log(fromData)
    return http.post('/api/auth', fromData)
  }

  logout() {
    return http.delete('/api/auth').then((res) => {
      store.dispatch('user/updateSession', null)
      return res
    })
  }

  updateAvatar(data) {
    return http.post('/api/users/settings/avatar', data)
  }

  updateBackground(data) {
    return http.post('/api/users/settings/background', data)
  }

  updateInfo(id, data) {
    return http.put(`/api/users/${ id }`, data)
  }

  updateEmail(data) {
    return http.put('/api/users/settings/email', data)
  }

  updatePassword(data) {
    return http.put('/api/users/settings/password', data)
  }

  getUserComments(page) {
    return http.get(`/api/comments/users?page=${ page }`)
  }

  getById(id) {
    return http.get(`/api/users/${ id }`)
  }
}
