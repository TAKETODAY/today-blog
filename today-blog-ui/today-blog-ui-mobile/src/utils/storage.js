import store from 'src/store'

function saveSession(key, obj) {
  sessionStorage.setItem(key, JSON.stringify(obj));
}

function getSession(key) {
  return JSON.parse(sessionStorage.getItem(key));
}

function clearStorage() {
  localStorage.clear();
  sessionStorage.clear();
  // message.message.success("缓存清除成功")
}

function removeSession(key) {
  if (getSession(key) != null) {
    sessionStorage.removeItem(key);
    return true;
  }
  return false;
}

function saveStorage(key, obj) {
  var str = JSON.stringify(obj); // 将对象转换为字符串
  localStorage.setItem(key, str);
}

function getStorage(key) {
  return JSON.parse(localStorage.getItem(key));
}

function removeStorage(key) {
  if (getStorage(key) != null) {
    localStorage.removeItem(key);
    return true;
  }
  return false;
}

function unauthorized() {
  store.dispatch('user/updateSession', null)
}

export {
  getSession,
  getStorage,
  saveSession,
  saveStorage,
  clearStorage,
  removeSession,
  removeStorage,
  unauthorized
};
