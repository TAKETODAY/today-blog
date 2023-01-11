export function isExist(value) {
  return value !== undefined && value !== null
}

export function isTrue(value) {
  return 'true' === value || true === value
}

export function isFalse(value) {
  return !isTrue(value)
}


export function isZero(value) {
  return isEmpty(value) || value === '0' || value === 0
}

export function isNull(object) {
  return object === null || object === undefined
}

export function isNotNull(object) {
  return !isNull(object)
}

export function isEmpty(item) {
  return !isNotEmpty(item)
}

export function isNotEmpty(item) {
  return item && item.length !== 0
}

export function hasProperty(object, deleteNullKeys = true) {
  if (deleteNullKeys) {
    deleteIfValueIsEmpty(object)
  }
  return isNotEmpty(Object.keys(object))
}

export function convertEmpty(param, target = null) {
  if (isNotEmpty(param)) {
    for (const [key, value] of Object.entries(param)) {
      if (isEmpty(value)) {
        param[key] = target
      }
    }
  }
}

export function deleteIfValueIsEmpty(object) {
  ifValueEmpty(object, key => {
    delete object[key]
  })
  return object
}

export function ifValueEmpty(object, callback) {
  if (isNotNull(object)) {
    for (const [key, value] of Object.entries(object)) {
      if (isEmpty(value)) {
        callback(key)
      }
    }
  }
  return object
}
