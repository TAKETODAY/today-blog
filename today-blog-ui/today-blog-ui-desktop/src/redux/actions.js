import {
  UPDATE_CATEGORIES, UPDATE_LABELS, UPDATE_NAVIGATION, UPDATE_OPTIONS, UPDATE_POPULAR,
  UPDATE_USER_SESSION, UPDATE_WINDOW_RESIZE, UPDATE_HTTP_ERROR_MESSAGE, CLEAR_HTTP_ERROR_MESSAGE
} from './action-types';

export const updateNavigations = (navigations = []) => ({
  type: UPDATE_NAVIGATION, data: navigations
})

export const updateUserSession = (user) => ({
  type: UPDATE_USER_SESSION, data: user
})

export const updateOptions = (options) => ({
  type: UPDATE_OPTIONS, data: options
})

export const updateCategories = (categories) => ({
  type: UPDATE_CATEGORIES, data: categories
})

export const updateLabels = (labels) => ({
  type: UPDATE_LABELS, data: labels
})

export const updatePopularArticles = (articles) => ({
  type: UPDATE_POPULAR, data: articles
})

export const updateWindowSize = (width) => ({
  type: UPDATE_WINDOW_RESIZE, data: width
})

export const updateHttpErrorMessage = (error) => ({
  type: UPDATE_HTTP_ERROR_MESSAGE, data: error
})

export const clearHttpErrorMessage = (key) => ({
  type: CLEAR_HTTP_ERROR_MESSAGE, key
})
