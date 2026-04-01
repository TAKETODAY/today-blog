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

import { updateOptions } from "src/redux/actions";
import { store } from 'src/redux/store';
import { useCallback } from "react";
import { updateNavigations, updateUserSession } from "../../redux/actions";
import { useLocation } from "react-router-dom";


export function useUserSession() {
  const { session } = store.getState()['user'];

  const setUserSession = useCallback(user => {
    store.dispatch(updateUserSession(user))
  }, [])

  return [session, setUserSession]
}

export function useOptions() {
  const { options } = store.getState()
  const setOptions = useCallback(options => {
    store.dispatch(updateOptions(options))
  }, [])
  return [options, setOptions]
}

export function useCDN() {
  const [options] = useOptions()
  return options['site.cdn']
}

export function useQueryParams() {
  const location = useLocation()
  return Object.fromEntries(new URLSearchParams(location.search))
}

export function useBreadcrumb() {
  const { navigation } = store.getState();

  const setNavigations = useCallback(nav => {
    store.dispatch(updateNavigations(nav))
  }, [])

  return [navigation, setNavigations]
}

//export function useBreadcrumb() {
//  const [navigations, setNavigations] = useState([])
//  console.log("navigations", navigations)
//  return [navigations, setNavigations]
//}

