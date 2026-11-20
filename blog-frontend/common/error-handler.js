import { unauthorized } from "./storage"
import { isNull } from "./object";
import { message, notification } from "antd";
import { translate } from './locales';

const errorMessage = (err, message) => {
  err.message = (err && err.response && err.response.data)
    ? err.response.data.detail || err.response.data.message || message : message
}

const handleNotFound = (err) => {
  errorMessage(err, translate('http.resourceNotFound'))
}

const handleBadRequest = (err) => {
  errorMessage(err, translate('http.badRequest'))
}

const handleUnauthorized = (err) => {
  errorMessage(err, translate('http.signInAndTryAgain'))
  unauthorized()
}

const handleAccessForbidden = (err) => {
  errorMessage(err, translate('http.accessDenied'))
}

const handleTooManyRequests = (err) => {
  errorMessage(err, translate('http.tooManyRequests'))
}

const handleInternalServerError = (err) => {
  errorMessage(err, translate('http.internalServerError'))
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
      errorMessage(err, translate('http.theServiceIsUnavailable'))
      notification.error({
        message: translate('http.error'),
        description: err.message
      })
    }
    else {
      errorHandler(err)
    }
  }
  else {
    errorMessage(err, translate('http.couldNotReachTheServer'))
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
