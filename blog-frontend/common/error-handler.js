import { unauthorized } from "./storage"
import { isNull } from "./object";
import { message, notification } from "antd";

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data) ? err.response.data.message || message : message
}

const handleNotFound = (err) => {
  errorMessage(err, '资源不存在')
}

const handleBadRequest = (err) => {
  errorMessage(err, '请求错误')
}

const handleUnauthorized = (err) => {
  errorMessage(err, '登录超时')
  unauthorized()
}

const handleAccessForbidden = (err) => {
  errorMessage(err, '没有权限')
}

const handleTooManyRequests = (err) => {
  errorMessage(err, '操作频繁')
}

const handleInternalServerError = (err) => {
  errorMessage(err, '服务器内部错误')
}

const errorHandlers = {
  400: handleBadRequest,
  401: handleUnauthorized,
  403: handleAccessForbidden,
  404: handleNotFound,
  429: handleTooManyRequests,
  500: handleInternalServerError,
}

export const handleHttpError = (err) => {

  if (err && err.response) {
    err.status = err.response.status
    const errorHandler = errorHandlers[err.response.status]
    if (isNull(errorHandler)) {
      errorMessage(err, '后台服务不可用')
      notification.error({
        message: '错误',
        description: err.message
      })
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
