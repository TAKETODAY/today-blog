/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import { message } from "antd";
import { history } from 'umi';
import { isEmpty, isNotEmpty, isZero } from "@/utils/object"

export const fallbackImage = "data:image/png;base64,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"

const production = process.env.NODE_ENV === 'production'

export function isProduction() {
  return production
}

function arrayNotEquals(value1 = [], value2 = []) {
  return !arrayEquals(value1, value2)
}

function arrayEquals(value1 = [], value2 = []) {
  if (value1 === value2 || (value1 === null && value2 === null)) {
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

// 下面这个函数会拷贝所有自有属性的属性描述符
function assign(target, ...sources) {
  sources && sources.forEach(source => {
    debugger
    let descriptors = Object.keys(source).reduce((descriptors, key) => {
      descriptors[key] = Object.getOwnPropertyDescriptor(source, key);
      return descriptors;
    }, {});

    // Object.assign 默认也会拷贝可枚举的Symbols
    Object.getOwnPropertySymbols(source).forEach(sym => {
      let descriptor = Object.getOwnPropertyDescriptor(source, sym);
      if (descriptor.enumerable) {
        descriptors[sym] = descriptor;
      }
    });
    Object.defineProperties(target, descriptors);
  });
  return target;
}

export function currentTimeMillis() {
  return new Date().getTime()
}

const share = {
  site: "https://taketoday.cn",
  desc: "TODAY BLOG 代码是我心中的一首诗",
  image: "https://cdn.taketoday.cn/logo.png",
  summary: "TODAY BLOG 是记录我学习的博客。主要分享自己的心得体会,学习经验、建站经验、资源分享、知识分享、杂谈生活.",
}

function shareQQ(options) {
  options = assign({}, share, options)
  window.open(
    "http://connect.qq.com/widget/shareqq/index.html?url=" + options.url +
    "&title=" + options.desc +
    "&desc=" + options.desc +
    "&summary=" + options.summary +
    "&site=" + options.site +
    "&pics=" + options.image
  )
}

function shareWeiBo(options) {
  options = { ...share, ...options }
  window.open(
    "http://service.weibo.com/share/share.php?url=" + options.url +
    "&title=" + options.desc +
    "&pic=" + options.image +
    "&searchPic=true"
  )
}

function shareQQZone(options) {
  options = { ...share, ...options }
  window.open(
    "https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=" + options.url +
    "&title=" + options.desc +
    "&desc=" + options.desc +
    "&summary=" + options.summary +
    "&site=" + options.site +
    "&pics=" + options.image
  )
}

export function setTitle(title, subTitle = true) {
  let titleToUse = title
  if (title === undefined) {
    // titleToUse = store.getState().options["site.subTitle"]
  }
  subTitle && (titleToUse += " - " + store.getState().options["site.name"])
  window.document.title = titleToUse
}

export function isAdmin(user) {
  return user.blogger
}

export function computeSummary(str) {
  if (isNotEmpty(str)) {
    str = str.replace(/<\/?[^>]+>/g, ''); // 去除HTML tag
    str = str.replace(/(\n)/g, "");
    str = str.replace(/(\t)/g, "");
    str = str.replace(/(\r)/g, "");
    str = str.replace(/&nbsp;/ig, ''); // 去掉&nbsp;
    return str.length > 256 ? str.substring(0, 256) : str;
  }
}

export function extractData(res) {
  return res.data
}

export function getPath(location) {
  const { pathname, hash } = location || history.location
  if (isEmpty(pathname)) {
    return hash.substring(1)
  }
  return pathname
}

export function isLoginPage(location) {
  return !isNotLoginPage(location)
}

export function shouldGoToLogin(location) {
  const path = getPath(location)
  return path !== '/user/login';
}

export function isNotLoginPage(location) {
  const path = getPath(location)
  return path !== '/user/login';
}

export function isNotHomePage(location) {
  const path = getPath(location)
  return path !== '/';
}

export function goToLogin() {
  const path = getPath()
  if (path !== '/') {
    history.push(`/user/login?redirect=${encodeURI(path)}`)
  }
  else {
    history.push('/user/login')
  }
}

export function getSizeString(value) {
  if (isZero(value)) {
    return "0 Bytes"
  }
  const srcSize = parseFloat(value);
  const index = Math.floor(Math.log(srcSize) / Math.log(1024));
  let size = srcSize / Math.pow(1024, index);
  size = size.toFixed(2);//保留的小数位数
  return size + unitArr[index];
}

const unitArr = ["Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"]

export function getSize(value) {
  if (isZero(value)) {
    return "0"
  }
  const srcSize = parseFloat(value);
  const index = Math.floor(Math.log(srcSize) / Math.log(1024));
  let size = srcSize / Math.pow(1024, index);
  size = size.toFixed(2);//保留的小数位数
  return size
}

export function getSizeUnit(value) {
  if (isZero(value)) {
    return "Bytes"
  }
  const index = Math.floor(Math.log(parseFloat(value)) / Math.log(1024))
  return unitArr[index]
}

export const NORMAL = "NORMAL"
export const LOCKED = "LOCKED"
export const RECYCLE = "RECYCLE"
export const INACTIVE = "INACTIVE"

export function getUserStatusDesc(status) {
  switch (status) {
    case NORMAL:
      return '正常'
    case LOCKED:
      return '账号被锁'
    case RECYCLE:
      return '账号被冻结'
    case INACTIVE:
      return '账号尚未激活'
    default:
      return '未知'
  }
}

export const DRAFT = "DRAFT"
export const PUBLISHED = "PUBLISHED"
export const POST_RECYCLE = RECYCLE

export function getPostStatusDesc(status) {
  switch (status) {
    case PUBLISHED:
      return '已发布'
    case DRAFT:
      return '草稿'
    case POST_RECYCLE:
      return '回收站'
    default:
      return '未知'
  }
}

export const COMMENT_RECYCLE = RECYCLE
export const COMMENT_CHECKED = "CHECKED"
export const COMMENT_CHECKING = "CHECKING"

export function getCommentStatusDesc(status) {
  switch (status) {
    case COMMENT_CHECKED:
      return '已审核'
    case COMMENT_CHECKING:
      return '未审核'
    case COMMENT_RECYCLE:
      return '回收站'
    default:
      return '未知'
  }
}

//
export const IMAGE = 'IMAGE'
export const AUDIO = 'AUDIO'
export const VIDEO = 'VIDEO'
export const TEXT = 'TEXT'
export const OTHER = 'OTHER'

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
  arrayNotEquals
};
