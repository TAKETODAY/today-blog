import http from 'axios';
import NProgress from 'nprogress';
import { unauthorized } from "./storage";
import { handleHttpError } from './error-handler';
import { currentTimeMillis } from "@/utils/common";

// export default
NProgress.configure({
  minimum: 0.2,
  ease: 'ease',
  speed: 500,
  trickleRate: 0.1,
  showSpinner: false
})

// Response Schema
// {
// `data` is the response that was provided by the server
// data: {},
// `status` is the HTTP status code from the server response
// status: 200,
// `statusText` is the HTTP status message from the server response
// statusText: 'OK',
// `headers` the HTTP headers that the server responded with
// All header names are lower cased and can be accessed using the bracket notation.
// Example: `response.headers['content-type']`
// headers: {},
// `config` is the config that was provided to `axios` for the request
// config: {},
// `request` is the request that generated this response
// It is the last ClientRequest instance in node.js (in redirects)
// and an XMLHttpRequest instance in the browser
// request: {}
// }

// const apiServer = 'http://localhost:8080'
// const apiServer = 'http://192.168.43.217:8080'
// http.defaults.baseURL = apiServer
http.defaults.withCredentials = true
http.defaults.timeout = 5000
// http.defaults.headers.common['Authorization'] = AUTH_TOKEN;
// http.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

// Set config defaults when creating the instance
// const instance = axios.create({
//   baseURL: 'https://api.example.com'
// });

// Alter defaults after instance has been created
// instance.defaults.headers.common['Authorization'] = AUTH_TOKEN;

// Add a request interceptor
http.interceptors.request.use(function (config) {
  // Do something before request is sent
  startNProgress()
  config.timeoutErrorMessage = "请求服务器超时"
  return config;
}, function (error) {
  stopNProgress()
  // Do something with request error
  return Promise.reject(error);
});

// Add a response interceptor
http.interceptors.response.use(function (response) {
  stopNProgress()
  return response;
}, function (err) {
  stopNProgress()
  handleHttpError(err)
  return Promise.reject(err);
});

const DEFAULT_CACHED_TIMEOUT = 10 * 1000  // 10s

export async function getCacheable(url) {
  startNProgress()

  let cached = sessionStorage.getItem(url)
  if (cached) {
    cached = JSON.parse(cached);
    if (currentTimeMillis() - cached.expired < DEFAULT_CACHED_TIMEOUT) {
      stopNProgress()
      return cached
    }
  }
  return http.get(url).then((res) => {
    res['expired'] = currentTimeMillis()
    sessionStorage.setItem(url, JSON.stringify(res))
    return res
  }).finally(() => stopNProgress())
}

export function startNProgress() {
  if (!NProgress.isStarted()) {
    NProgress.start()
  }
}

export function stopNProgress() {
  NProgress.done()
}

function getQuery(name, search) {
  return new URLSearchParams(search || window.location.search).get(name)
}


export { http, getQuery };

