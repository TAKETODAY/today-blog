import { useModel } from "@@/plugin-model/useModel"


import { useState, useCallback } from 'react'

export default function useAuthModel() {
  const [user, setUser] = useState(null)

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

export function useUserSession() {
  const { initialState, setInitialState } = useModel("@@initialState")
  const { currentUser: userSession } = initialState;

  const setUserSession = async currentUser => {
    await setInitialState({ ...initialState, currentUser })
  }

  return [userSession, setUserSession]
}
