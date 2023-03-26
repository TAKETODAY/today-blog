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

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data) ? err.response.data.message || message : message
  notification.warn({
    message: '提示',
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
      title: "该操作需要登录",
      content: <span style={{ color: "red" }}>系统检测到: 您尚未登陆,或者在线时间超时<br/>
              <strong>如果有数据未保存请在新窗口打开系统登录</strong>
            </span>,
      cancelText: '取消',
      okText: '去登录',
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
  errorMessage(err, '资源不存在')
}

const handleBadRequest = (err) => {
  errorMessage(err, '请求错误')
}

const handleUnauthorized = (err) => {
  if (!showLoginDialog()) {
    errorMessage(err, '登录超时')
  }
}

const handleAccessForbidden = (err) => {
  errorMessage(err, '没有权限')
}

const handleInternalServerError = (err) => {
  errorMessage(err, '服务器内部错误')
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
      errorMessage(err, '后台服务不可用')
    }
    else {
      errorHandler(err)
    }
  }
  else {
    errorMessage(err, '错误未知')
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
