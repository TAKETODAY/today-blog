import { useModel } from "@umijs/max"

import { useCallback, useState } from 'react'

export default function useAuthModel() {
  const [user] = useState(null)

  const signin = useCallback((account, password) => {
    // signin implementation
    // setUser(user from signin API)
  }, [])

  const signout = useCallback(() => {
    // signout implementation
    // setUser(null)
  }, [])

  return {
    user,
    signin,
    signout
  }
}

export function useUserSession(): [API.CurrentUser, (currentUser: API.CurrentUser) => Promise<void>] {
  const { initialState, setInitialState } = useModel("@@initialState")
  const { currentUser: userSession } = initialState;

  const setUserSession = useCallback(async currentUser => {
    return setInitialState({ ...initialState, currentUser })
  }, [initialState])

  return [userSession, setUserSession]
}
