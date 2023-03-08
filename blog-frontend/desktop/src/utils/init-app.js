/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import { getCacheable, http } from "./http";
import {
  updateCategories,
  updateHttpErrorMessage,
  updateLabels,
  updateOptions,
  updatePopularArticles,
  updateUserSession
} from "../redux/actions";
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
    consoleLog(options)

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

const consoleLog = (options) => {
  console.log(`%cTODAY BLOG%c\n代码是我心中的一首诗\n\n${options['site.copyright']}\n`,
    "font-size:96px;text-shadow: 1px 1px 1px rgba(0,0,0,.2);",
    "font-size:12px;color:rgba(0,0,0,.38);")
}

export default [
  initStore
]
