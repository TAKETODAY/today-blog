import { combineReducers } from 'redux'; // 利用combineReducers 合并reducers
import { isEmpty } from '../utils';
import {
  APP_INIT,
  UPDATE_CATEGORIES,
  UPDATE_LABELS,
  UPDATE_NAVIGATION,
  UPDATE_OPTIONS,
  UPDATE_POPULAR,
  UPDATE_USER_SESSION,
  UPDATE_WINDOW_RESIZE,
  UPDATE_HTTP_ERROR_MESSAGE, CLEAR_HTTP_ERROR_MESSAGE,
} from './action-types';

/**
 * 用于面包屑更新
 * @param { *} state
 * @param {*} action
 */
export const navigationReducer = (state = [], action) => {

  switch (action.type) {
    case UPDATE_NAVIGATION:
      if (isEmpty(action.data)) {
        return []
      }
      return [...action.data]
    default:
      return state
  }
}

/**
 * 用于user
 * @param {*} state
 * @param {*} action
 */
export const userReducer = (state = {}, action) => {

  switch (action.type) {
    case UPDATE_USER_SESSION:
      localStorage.setItem('session', JSON.stringify(action.data))
      return { ...state, session: action.data }
    default:
      return state
  }
}

const options = {
  'site.icp': '',
  'site.copyright': 'Copyright © TODAY & 2017 - 2023 All Rights Reserved.',
  'site.otherFooter': '',
  'site.image.server': ''
};

export const optionsReducer = (state = options, action) => {

  switch (action.type) {
    case UPDATE_OPTIONS:
      return { ...state, ...action.data }
    case APP_INIT:
      return { ...state, ...options }
    case UPDATE_WINDOW_RESIZE:
      return { ...state, windowWidth: action.data }
    default:
      return state
  }
}

/**
 * 用于categories
 * @param {*} state
 * @param {*} action
 */
export const articleReducer = (state = {}, action) => {

  switch (action.type) {
    case UPDATE_CATEGORIES:
      return { ...state, categories: action.data }
    case UPDATE_LABELS:
      return { ...state, labels: action.data }
    case UPDATE_POPULAR:
      return { ...state, popular: action.data }
    default:
      return state
  }
}

/**
 * 用于http
 * @param {*} state
 * @param {*} action
 */
export const httpReducer = (state = {}, action) => {
  switch (action.type) {
    case APP_INIT:
      return { ...state, errors: {} }
    case UPDATE_HTTP_ERROR_MESSAGE:
      // 叠加 errors
      return { errors: { ...state.errors, ...action.data } }
    case CLEAR_HTTP_ERROR_MESSAGE: {
      const { key } = action
      let errors = { ...state.errors }
      // 删除 errors 中的 key
      delete errors[key]
      return { errors }
    }
    default:
      return state
  }
}


export default combineReducers({
  user: userReducer,
  http: httpReducer,
  options: optionsReducer,
  article: articleReducer,
  navigation: navigationReducer
})
