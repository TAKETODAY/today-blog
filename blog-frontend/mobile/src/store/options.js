const defaultOptions = {
  'site.name': 'TODAY BLOG',
  'site.icp': '',
  'site.copyright': 'Copyright © TODAY & 2017 - 2023 All Rights Reserved.',
  'site.otherFooter': '代码是我心中的一首诗',
  'site.image.server': ''
};

const state = () => ({
  ...defaultOptions
})

const mutations = {

  setOptions(state, options) {
    Object.assign(state, options)
  },
}

const actions = {

  updateOptions({ state, commit, rootState }, opt) {
    commit("setOptions", opt)
  },
}

const getters = {
  options(state/*, getters, rootState*/) {
    return state
  }
}

export default {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
