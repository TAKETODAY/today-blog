import store from "store";
import { Dialog, Toast } from "vant";
import { toLogin } from "./http";

function arrayNotEquals(value1 = [], value2 = []) {
  return !arrayEquals(value1, value2)
}

function arrayEquals(value1 = [], value2 = []) {
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

function isEmpty(item) {
  return !isNotEmpty(item)
}

function isNotEmpty(item) {
  return item && item.length !== 0
}

export function currentTimeMillis() {
  return new Date().getTime()
}

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


const share = {
  url: "https://taketoday.cn",
  site: "https://taketoday.cn",
  desc: "TODAY BLOG 代码是我心中的一首诗",
  image: "https://cdn.taketoday.cn/logo.png",
  summary: "TODAY BLOG 是记录我学习的博客。主要分享自己的心得体会,学习经验、建站经验、资源分享、知识分享、杂谈生活.",
}

function shareQQ(options) {
  options = { ...share, ...options }
  windowOpen("http://connect.qq.com/widget/shareqq/index.html?url=" + options.url +
      "&title=" + options.desc +
      "&desc=" + options.desc +
      "&summary=" + options.summary +
      "&site=" + options.site +
      "&pics=" + options.image
  )
}

function shareWeiBo(options) {
  options = { ...share, ...options }
  windowOpen("http://service.weibo.com/share/share.php?url=" + options.url +
      "&title=" + options.desc +
      "&pic=" + options.image +
      "&searchPic=true"
  )
}

function windowOpen(url) {
  let indexOf = url.indexOf('?');
  if (indexOf > -1) {
    const query = encodeURI(url.substr(indexOf + 1))
    window.open(url.substr(0, indexOf + 1) + query)
  }
  else {
    window.open(url)
  }
}

function shareQQZone(options) {
  options = { ...share, ...options }
  windowOpen("https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=" + options.url +
      "&title=" + options.desc +
      "&desc=" + options.desc +
      "&summary=" + options.summary +
      "&site=" + options.site +
      "&pics=" + options.image
  )
}


export function setClipboard(text) {
  try {
    navigator.clipboard.writeText(text).then(() => {
      Toast("复制成功")
    }).catch(() => {
      Toast("复制失败")
    })
  }
  catch (e) {
    copy(text)
  }
}

function copy(text) {
  let transfer = document.createElement('input');
  document.body.appendChild(transfer);
  transfer.value = text;  // 这里表示想要复制的内容
  transfer.select();
  if (document.execCommand('copy')) {
    document.execCommand('copy');
  }
  Toast("复制成功")
  document.body.removeChild(transfer);
}

function requireLogin(vue, err) {
  if (err.status === 401) {
    showLoginDialog(vue)
  }
  else {
    Toast(err.message)
  }
}

function handleValidationError(err, callback) {
  if (err.status === 400 && err.data.validation) {
    Toast(Object.values(err.data.validation)[0]);
    callback(err.data.validation)
  }
}

function showLoginDialog({ $router, $route }) {
  Dialog.alert({
    title: "您还未登陆",
    message: "该操作需要登录",
    showCancelButton: true,
    cancelButtonText: '取消',
    confirmButtonText: '去登录',
  }).then((res) => {
    toLogin({ $router, $route })
  }).catch(() => {
  })
}

function cartTotalPrice(items) {
  return items.reduce((total, item/*, idx, items*/) =>
      item.checked ? (total + item.totalPrice) : total, 0
  )
}

export function getSiteName() {
  return store.getters["options.site.name"] || "TODAY BLOG"
}

export function setTitle(title, subTitle = true) {
  let titleToUse = title
  if (title === undefined) {
    titleToUse = store.getters["options.site.subTitle"]
  }

  titleToUse += " - " + getSiteName()
  if (titleToUse) {
    window.document.title = titleToUse
  }
}

export function logging(...data) {
  console.log(...data)
}

export {
  shareQQ,
  shareQQZone,
  shareWeiBo,
  isEmpty,
  isNotEmpty,
  arrayEquals,
  arrayNotEquals,
  showLoginDialog,
  requireLogin,
  cartTotalPrice,
  handleValidationError
};
