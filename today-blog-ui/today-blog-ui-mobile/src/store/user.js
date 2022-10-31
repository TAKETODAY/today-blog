import store from "./index";
import { getSession, removeSession, saveSession } from 'src/utils';

const USER_SESSION_KEY = 'user-session'

const state = () => ({
  session: null
})

const mutations = {

  setSession(state, session) {
    state.session = session
    if (session == null) {
      removeSession(USER_SESSION_KEY)
    }
    else {
      saveSession(USER_SESSION_KEY, session)
    }
  },
}

const actions = {

  updateSession({ state, commit, rootState }, session) {
    commit("setSession", session)
  },
}

function findSession(state) {
  return state.session == null
      ? getSession(USER_SESSION_KEY)
      : state.session
}

const getters = {

  session(state/*, getters, rootState*/) {
    const session = findSession(state)
    if (state.session == null && session != null) {
      store.commit('user/setSession', session)
    }
    return session
  }
}

export default {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
