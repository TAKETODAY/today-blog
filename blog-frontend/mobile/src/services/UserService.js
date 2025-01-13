/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
    return http.post('/api/auth?v2', fromData)
  }

  logout() {
    return http.delete('/api/auth').then((res) => {
      store.dispatch('user/updateSession', null)
      return res
    })
  }

  updateAvatar(data) {
    return http.put('/api/auth?avatar', data)
  }

  updateBackground(data) {
    return http.put('/api/auth?background', data)
  }

  updateInfo(id, data) {
    return http.put(`/api/auth`, data)
  }

  updateEmail(data) {
    return http.put('/api/auth?email-mobile-phone', data)
  }

  updatePassword(data) {
    return http.put('/api/auth?password', data)
  }

  getUserComments(page) {
    return http.get(`/api/comments/users?page=${ page }`)
  }

  getById(id) {
    return http.get(`/api/users/${ id }`)
  }
}
