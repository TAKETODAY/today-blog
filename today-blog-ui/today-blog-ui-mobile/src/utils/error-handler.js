import { unauthorized } from "./storage"
import { Toast } from "vant";

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

export function handleError(err) {
  Toast.fail({ message: err.message });
}

export const handleHttpError = (err) => {

  if (err && err.response) {
    err.data = err.response.data
    err.status = err.response.status
    const errorHandler = errorHandlers[err.response.status]
    if (errorHandler === null) {
      errorMessage(err, '服务不可用')
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
