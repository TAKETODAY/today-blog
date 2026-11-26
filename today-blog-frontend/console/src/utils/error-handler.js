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

import { message, Modal, notification } from "antd";
import { getPath, goToLogin, isNotLoginPage, isNull } from "@/utils";
import React from "react"
import { Iterable } from "immutable"
import { siteText as t } from './site-i18n';

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data)
    ? err.response.data.detail || err.response.data.message || message : message
  notification.warn({
    message: t('errors.notice'),
    description: err.message
  })
}

let loginDialogOpened = false

const hideLoginDialogPath = [
  "/not-found",
  "/user/login"
]

export function showLoginDialog(loginCallback, onCancel) {
  if (loginDialogOpened) {
    // 已经打开
    return false
  }
  const path = getPath()
  if ("/" === path) {
    goToLogin()
    return true
  }
  if (hideLoginDialogPath.indexOf(path) < 0) {
    console.log("不在登录页面,可以展示提示框")
    Modal.confirm({
      title: t('errors.signInRequired'),
      content: <span style={{ color: 'red' }}>{t('errors.youAreSignedOutOr')}<br/>
              <strong>{t('errors.ifYouHaveUnsavedChanges')}</strong>
            </span>,
      cancelText: t('errors.cancel'),
      okText: t('errors.signIn'),
      onOk: () => {
        loginDialogOpened = false
        const isNotLogin = isNotLoginPage()
        if (isNotLogin) {
          goToLogin()
        }
        loginCallback && loginCallback(!isNotLogin)
      },
      onCancel: () => {
        onCancel && onCancel()
        loginDialogOpened = false
      },
    })

    loginDialogOpened = true
    // is show login dialog
    return true
  }
  console.log("当前就在登录页面,不展示提示框")
  return false
}

const handleNotFound = (err) => {
  errorMessage(err, t('errors.resourceNotFound'))
}

const handleBadRequest = (err) => {
  errorMessage(err, t('errors.badRequest'))
}

const handleUnauthorized = (err) => {
  if (!showLoginDialog()) {
    errorMessage(err, t('errors.sessionExpired'))
  }
}

const handleAccessForbidden = (err) => {
  errorMessage(err, t('errors.accessDenied'))
}

const handleInternalServerError = (err) => {
  errorMessage(err, t('errors.internalServerError'))
}

const errorHandlers = {
  400: handleBadRequest,
  401: handleUnauthorized,
  403: handleAccessForbidden,
  404: handleNotFound,
  500: handleInternalServerError,
}

export const handleHttpError = err => {
  if (err && err.response) {
    err.status = err.response.status
    const errorHandler = errorHandlers[err.status]
    if (isNull(errorHandler)) {
      errorMessage(err, t('errors.theServiceIsUnavailable'))
    }
    else {
      errorHandler(err)
    }
  }
  else {
    errorMessage(err, t('errors.couldNotReachTheServer'))
    err.status = -1
  }
}

//

export function showHttpErrorMessageVoid(err) {
  message.error(err.message)
}

export async function showHttpErrorMessage(err) {
  return message.error(err.message)
}

export function handleValidationError(err, other, validationCallback) {
  const { response } = err
  if (response && response.status === 400) {
    const { data } = response

    if (data) {
      const { validation } = data
      const validationMap = new Iterable(validation)
      const first = validationMap.first()
      if (first) {
        message.error(first)
        validationCallback && validationCallback(validation)
        return
      }
    }
  }

  other && other(err)
}

export function mergeValidationError(err) {
  const { data, response } = err
  if (response && response.status === 400 && data && data.validation) {
    const { validation } = data
    err.message = validation[0].message
  }
}
