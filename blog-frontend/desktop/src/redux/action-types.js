export const APP_INIT = '@@INIT'
export const UPDATE_OPTIONS = 'update-options'
export const UPDATE_NAVIGATION = 'update-navigation'
export const UPDATE_USER_SESSION = 'update-user-session'
export const UPDATE_CATEGORIES = 'update-categories'
export const UPDATE_LABELS = 'update-labels'
export const UPDATE_POPULAR = 'update-popular'

export const UPDATE_WINDOW_RESIZE = 'update-window-resize'
export const UPDATE_HTTP_ERROR_MESSAGE = 'updateHttpErrorMessage'
export const CLEAR_HTTP_ERROR_MESSAGE = 'clearHttpErrorMessage'

export function navigationsMapStateToProps(state) {
  return { navigations: state.navigation }
}

export function userSessionMapStateToProps(state) {
  return { userSession: state.user.session }
}

export function navigationsUserSessionMapStateToProps(state) {
  return { navigations: state.navigation, userSession: state.user.session }
}

export function optionsMapStateToProps(state) {
  return { options: state.options }
}

export function userSessionOptionsMapStateToProps(state) {
  return { userSession: state.user.session, options: state.options }
}

export function navigationsUserSessionOptionsMapStateToProps(state) {
  return { navigations: state.navigation, userSession: state.user.session, options: state.options }
}


