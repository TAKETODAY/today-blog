import { getCacheable, http } from "./http";
import { updateCategories, updateHttpErrorMessage, updateLabels, updateOptions, updatePopularArticles, updateUserSession } from "../redux/actions";
import { articleService } from "../services";
import { applySEO, setDefaultSEODescription, setDefaultSEOKeywords } from "./common";


function getError(err) {
  return {
    status: err.response ? err.response.status : 500,
    subTitle: err.message
  }
}

function dispatchHttpError(store, key, err) {
  const error = getError(err)
  const obj = {
    [key]: error
  }
  store.dispatch(updateHttpErrorMessage(obj))
}

const initStore = (store) => {

  http.get("/api/auth").then(res => {
    localStorage.setItem('session', JSON.stringify(res.data))
    store.dispatch(updateUserSession(res.data))
  })

  getCacheable("/api/options").then(res => {
    const options = res.data

    store.dispatch(updateOptions(options))
    setDefaultSEOKeywords(options['site.keywords'])
    setDefaultSEODescription(options['site.description'])

    applySEO()
  }).catch(err => {
    dispatchHttpError(store, 'options', err)
  })

  getCacheable("/api/categories").then(res => {
    store.dispatch(updateCategories(res.data))
  }).catch(err => {
    dispatchHttpError(store, 'categories', err)
  })

  getCacheable("/api/tags").then(res => {
    store.dispatch(updateLabels(res.data))
  }).catch(err => {
    dispatchHttpError(store, 'tags', err)
  })

  articleService.popularArticles().then(res => {
    store.dispatch(updatePopularArticles(res.data))
  }).catch(err => {
    dispatchHttpError(store, 'popularArticles', err)
  })

}

const consoleLog = () => {
  console.log("%cTODAY BLOG%c\n代码是我心中的一首诗\n\nCopyright © TODAY & 2017 - 2022 All Rights Reserved.\n",
      "font-size:96px;text-shadow: 1px 1px 1px rgba(0,0,0,.2);",
      "font-size:12px;color:rgba(0,0,0,.38);")
}

export default [
  consoleLog,
  initStore
]
