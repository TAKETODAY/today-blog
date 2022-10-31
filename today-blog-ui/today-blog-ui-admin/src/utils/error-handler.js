import { unauthorized } from "./storage"
import { notification } from "antd";
import { isEmpty, isNull } from "@/utils/common";

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
  notification.error({
    message :err.message
  })
  // unauthorized()
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
        message :err.message
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
