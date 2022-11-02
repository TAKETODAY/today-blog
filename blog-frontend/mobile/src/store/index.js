import Vue from 'vue'
import Vuex from 'vuex'
import user from './user'
import options from './options'

import createLogger from 'vuex/dist/logger'

Vue.use(Vuex);
const debug = process.env.NODE_ENV !== 'production'

// 挂载
export default new Vuex.Store({
  modules: {
    user,
    options,
  },
  state: {},
  mutations: {},
  actions: {},
  getters: {},
  strict: debug,
  plugins: debug ? [createLogger()] : []
});
