import { unauthorized } from "./storage"
import { isNull } from "./object";
import { message, notification } from "antd";
import { translate } from './visitor-preferences';

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data)
    ? err.response.data.detail || err.response.data.message || message : message
}

const handleNotFound = (err) => {
  errorMessage(err, translate('资源不存在', 'Resource not found'))
}

const handleBadRequest = (err) => {
  errorMessage(err, translate('请求错误', 'Bad request'))
}

const handleUnauthorized = (err) => {
  errorMessage(err, translate('请登录后重试', 'Sign in and try again'))
  unauthorized()
}

const handleAccessForbidden = (err) => {
  errorMessage(err, translate('没有权限', 'Access denied'))
}

const handleTooManyRequests = (err) => {
  errorMessage(err, translate('操作频繁', 'Too many requests'))
}

const handleInternalServerError = (err) => {
  errorMessage(err, translate('服务器内部错误', 'Internal server error'))
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
      errorMessage(err, translate('后台服务不可用', 'The service is unavailable'))
      notification.error({
        message: translate('错误', 'Error'),
        description: err.message
      })
    }
    else {
      errorHandler(err)
    }
  }
  else {
    errorMessage(err, translate('无法连接到服务器', 'Could not reach the server'))
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
