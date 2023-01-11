import { goToLogin, isNotLoginPage, isNull } from "@/utils";
import { message, Modal, notification } from "antd";

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data) ? err.response.data.message || message : message
}

const handleNotFound = (err) => {
  errorMessage(err, '资源不存在')
}

const handleBadRequest = (err) => {
  errorMessage(err, '请求错误')
}

export function showLoginDialog(loginCallback, onCancel) {
  if (isNotLoginPage()) {
    console.log("不在登录页面,可以展示提示框")
    Modal.confirm({
      title: "该操作需要登录",
      content: <span style={{ color: "red" }}>系统检测到: 您尚未登陆,或者在线时间超时<br/>
              <strong>如果有数据未保存请在新窗口打开系统登录</strong>
            </span>,
      cancelText: '取消',
      okText: '去登录',
      onOk: () => {
        const isNotLogin = isNotLoginPage()
        if (isNotLogin) {
          goToLogin()
        }
        loginCallback && loginCallback(!isNotLogin)
      },
      onCancel: onCancel,
    })

    // is show login dialog
    return true
  }
  console.log("当前就在登录页面,不展示提示框")
  return false
}

const handleUnauthorized = (err) => {
  if (!showLoginDialog()) {
    errorMessage(err, '登录超时')
    notification.error({
      message: err.message
    })
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

export const handleHttpError = (err) => {

  if (err && err.response) {
    err.status = err.response.status
    const errorHandler = errorHandlers[err.response.status]
    if (isNull(errorHandler)) {
      errorMessage(err, '服务不可用')
      notification.error({
        message: err.message
      })
    }
    else {
      errorHandler(err)
    }
  }
  else {
    errorMessage(err, '错误未知')
    err.status = -1
    err.data = null
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
