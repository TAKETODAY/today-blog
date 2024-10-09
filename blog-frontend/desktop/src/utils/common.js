/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { store } from "../redux/store";
import { isEmpty, isNotEmpty, isZero } from "./object";
import moment from 'moment';
import sha256 from 'js-sha256';

export function currentTimeMillis() {
  return new Date().getTime()
}

const SMALL_WINDOW_WIDTH = 860;

export function isSmallWindow(width) {
  return width <= SMALL_WINDOW_WIDTH
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


const share = {
  site: "https://taketoday.cn",
  desc: "TODAY BLOG 代码是我心中的一首诗",
  cover: "https://cdn.taketoday.cn/logo.png",
  summary: "TODAY BLOG 是记录我学习的博客。主要分享自己的心得体会,学习经验、建站经验、资源分享、知识分享、杂谈生活.",
}

export function shareQQ(options) {
  options = Object.assign({}, share, options)
  window.open(
    "http://connect.qq.com/widget/shareqq/index.html?url=" + options.url +
    "&title=" + options.desc +
    "&desc=" + options.desc +
    "&summary=" + options.summary +
    "&site=" + options.site +
    "&pics=" + options.cover
  )
}

export function shareWeiBo(options) {
  options = { ...share, ...options }
  window.open(
    "http://service.weibo.com/share/share.php?url=" + options.url +
    "&title=" + options.desc +
    "&pic=" + options.cover +
    "&searchPic=true"
  )
}

export function shareQQZone(options) {
  options = { ...share, ...options }
  window.open(
    "https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=" + options.url +
    "&title=" + options.desc +
    "&desc=" + options.desc +
    "&summary=" + options.summary +
    "&site=" + options.site +
    "&pics=" + options.cover
  )
}

export function getSiteName() {
  return store.getState().options["site.name"] || "TODAY BLOG"
}

export function setTitle(title, subTitle = true) {
  let titleToUse = title
  if (title === undefined) {
    titleToUse = store.getState().options["site.subTitle"]
  }

  titleToUse += " - " + getSiteName()
  if (titleToUse) {
    window.document.title = titleToUse
  }
}

export function getForward() {
  return window.location.pathname
}

export function logging(...data) {
  console.log(...data)
}

export function getSummary(str) {
  if (isNotEmpty(str)) {
    str = str.replace(/<\/?[^>]+>/g, ''); // 去除HTML tag
    str = str.replace(/(\n)/g, "");
    str = str.replace(/(\t)/g, "");
    str = str.replace(/(\r)/g, "");
    str = str.replace(/&nbsp;/ig, ''); // 去掉&nbsp;
    return str.length > 256 ? str.substring(0, 256) : str;
  }
}

export function getMeta() {
  let ret = {}
  const metaElements = document.querySelectorAll('meta');
  metaElements.forEach(metaElement => {
    const name = metaElement.getAttribute('name')
    if ('keywords' === name) {
      ret['keywords'] = metaElement
    }
    else if ('description' === name) {
      ret['description'] = metaElement
    }
  })

  if (isEmpty(ret['keywords'])) {
    const metaElement = document.createElement('meta');
    metaElement.name = 'keywords'
    ret['keywords'] = metaElement
    const headElement = document.querySelector("head");
    headElement.append(metaElement)
  }
  if (isEmpty(ret['description'])) {
    const metaElement = document.createElement('meta');
    metaElement.name = 'description'
    ret['description'] = metaElement
    const headElement = document.querySelector("head");
    headElement.append(metaElement)
  }
  return ret
}

//杨海健的个人网站，包括博客、开发工具和开源项目等，欢迎访问。
//TODAY BLOG 代码是我心中的一首诗，电子，编程，Java，分享，STM32，51单片机，ARM，杨海健，心得
let defaultKeywords = ''
let defaultDescription = ''

export function setDefaultSEOKeywords(keywords) {
  defaultKeywords = keywords
}

export function setDefaultSEODescription(description) {
  defaultDescription = description
}

export function applySEO(keywords = defaultKeywords, description = defaultDescription) {
  const meta = getMeta();
  const keywordsElement = meta['keywords']
  keywordsElement.content = isEmpty(keywords) ? defaultKeywords : keywords
  // description
  const descriptionEle = meta['description']
  descriptionEle.content = isEmpty(description) ? defaultDescription : description
}

export function getArticleId(id) {
  if (id && id.endsWith('.html')) {
    return id.substr(0, id.length - '.html'.length)
  }
  return id
}

export function extractData(res) {
  return res.data
}

export function format(time, format = "llll") {
  return toLocalTime(time).format(format)
}

export function fromNow(time) {
  return toLocalTime(time).fromNow()
}

export function toLocalTime(time) {
  return moment.utc(time).local()
}

export function getGravatarURL(email, size = "40") {
  // Trim leading and trailing whitespace from
  // an email address and force all characters
  // to lower case
  const address = String(email).trim().toLowerCase();

  // Create a SHA256 hash of the final string
  const hash = sha256(address);
  // Grab the actual image URL
  return `https://www.gravatar.com/avatar/${hash}?d=identicon&s=${size}`;
}
