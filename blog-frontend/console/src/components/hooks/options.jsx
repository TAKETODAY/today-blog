/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { useModel } from "@@/plugin-model/useModel"
import { useCallback } from "react";

// model
// error: undefined
// initialState: {options: {…}, currentUser: {…}, settings: {…}, fetchUserInfo: ƒ}
// loading: false
// refresh: ƒ ()
// setInitialState: ƒ (_x)

export function useOptions() {
  const { initialState, setInitialState } = useModel("@@initialState")

  const setOptions = useCallback(async options => {
    await setInitialState({ ...initialState, options })
  }, [initialState])

  const options = (initialState && initialState.options) || {}
  return [options, setOptions]
}

export function useCDN() {
  const [options] = useOptions()
  return options['site.cdn']
}
