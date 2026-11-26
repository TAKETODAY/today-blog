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

export function hasProperty(object, deleteNullKeys) {
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

export function arrayNotEquals(value1 = [], value2 = []) {
  return !arrayEquals(value1, value2)
}

export function arrayEquals(value1 = [], value2 = []) {
  if (value1 === value2 || (value1 === null, value2 === null)) {
    return true
  }
  if (isNotEmpty(value1) && isNotEmpty(value2)) {
    if (value1.length === value2.length) {
      for (let i = 0; i < value1.length; i++) {
        if (value1[i] !== value2[i]) {
          // console.log("JSON->", JSON.stringify(value1[i]), JSON.stringify(value2[i]))
          // if (JSON.stringify(value1[i]) !== JSON.stringify(value2[i])) {
          return false
        }
      }
      return true
    }
  }
  return false
}
