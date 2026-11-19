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
import { siteText as t } from './site-format';

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data)
    ? err.response.data.detail || err.response.data.message || message : message
  notification.warn({
    message: t('提示', 'Notice'),
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
      title: t('该操作需要登录', 'Sign-in required'),
      content: <span style={{ color: 'red' }}>{t('您尚未登录，或会话已过期。', 'You are signed out, or your session has expired.')}<br/>
              <strong>{t('如果有未保存的内容，请在新窗口登录。', 'If you have unsaved changes, sign in in a new window.')}</strong>
            </span>,
      cancelText: t('取消', 'Cancel'),
      okText: t('去登录', 'Sign in'),
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
  errorMessage(err, t('资源不存在', 'Resource not found'))
}

const handleBadRequest = (err) => {
  errorMessage(err, t('请求错误', 'Bad request'))
}

const handleUnauthorized = (err) => {
  if (!showLoginDialog()) {
    errorMessage(err, t('登录超时', 'Session expired'))
  }
}

const handleAccessForbidden = (err) => {
  errorMessage(err, t('没有权限', 'Access denied'))
}

const handleInternalServerError = (err) => {
  errorMessage(err, t('服务器内部错误', 'Internal server error'))
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
      errorMessage(err, t('后台服务不可用', 'The service is unavailable'))
    }
    else {
      errorHandler(err)
    }
  }
  else {
    errorMessage(err, t('无法连接到服务器', 'Could not reach the server'))
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
