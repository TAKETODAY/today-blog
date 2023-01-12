import { useModel } from "@@/plugin-model/useModel"

// model
// error: undefined
// initialState: {options: {…}, currentUser: {…}, settings: {…}, fetchUserInfo: ƒ}
// loading: false
// refresh: ƒ ()
// setInitialState: ƒ (_x)

export function useOptions() {
  const { initialState, setInitialState } = useModel("@@initialState")
  const setOptions = async options => {
    await setInitialState({ ...initialState, options })
  }
  const { options } = initialState
  return [options, setOptions]
}
