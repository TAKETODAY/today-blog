import { isNotEmpty } from "./common";

const echo = {};

let callback = function () {

}

let offset, poll, delay, useDebounce, unload;

const isHidden = function (element) {
  return (element.offsetParent === null);
};

const inView = function (element, view) {
  if (isHidden(element)) {
    return false;
  }

  const box = element.getBoundingClientRect();
  return (box.right >= view.l && box.bottom >= view.t && box.left <= view.r && box.top <= view.b);
};

const debounceOrThrottle = function () {
  if (!useDebounce && !!poll) {
    return;
  }
  clearTimeout(poll);
  poll = setTimeout(function () {
    echo.render();
    poll = null;
  }, delay);
};

echo.init = function (opts) {
  opts = opts || {};
  const offsetAll = opts.offset || 0;
  const offsetVertical = opts.offsetVertical || offsetAll;
  const offsetHorizontal = opts.offsetHorizontal || offsetAll;
  const optionToInt = function (opt, fallback) {
    return parseInt(opt || fallback, 10);
  };
  offset = {
    t: optionToInt(opts.offsetTop, offsetVertical),
    b: optionToInt(opts.offsetBottom, offsetVertical),
    l: optionToInt(opts.offsetLeft, offsetHorizontal),
    r: optionToInt(opts.offsetRight, offsetHorizontal)
  };
  delay = optionToInt(opts.throttle, 250);
  useDebounce = opts.debounce !== false;
  unload = !!opts.unload;
  callback = opts.callback || callback;
  echo.render();
  if (document.addEventListener) {
    document.addEventListener('scroll', debounceOrThrottle, false);
    document.addEventListener('load', debounceOrThrottle, false);
  }
  else {
    document.attachEvent('onscroll', debounceOrThrottle);
    document.attachEvent('onload', debounceOrThrottle);
  }
};

echo.render = function (context) {
  const nodes = (context || document).querySelectorAll('[data-original], [data-echo], [data-echo-background]');
  const length = nodes.length;

  let src, elem;
  const view = {
    l: 0 - offset.l,
    t: 0 - offset.t,
    b: (document.innerHeight || document.documentElement.clientHeight) + offset.b,
    r: (document.innerWidth || document.documentElement.clientWidth) + offset.r
  };

  for (var i = 0; i < length; i++) {
    elem = nodes[i];
    if (inView(elem, view)) {

      if (unload) {
        elem.setAttribute('data-echo-placeholder', elem.src);
      }
      if (elem.getAttribute('data-echo-background') !== null) {
        elem.style.backgroundImage = 'url(' + elem.getAttribute('data-echo-background') + ')';
      }
      else if (elem.src !== (src = getSrc(elem))) {
        elem.src = src;
      }

      if (!unload) {
        elem.removeAttribute('data-echo');
        elem.removeAttribute('data-original');
        elem.removeAttribute('data-echo-background');
      }

      callback(elem, 'load');
    }
    else if (unload && !!(src = elem.getAttribute('data-echo-placeholder'))) {

      if (elem.getAttribute('data-echo-background') !== null) {
        elem.style.backgroundImage = 'url(' + src + ')';
      }
      else {
        elem.src = src;
      }
      elem.removeAttribute('data-echo-placeholder');
      callback(elem, 'unload');
    }
  }
  if (!length) {
    echo.detach();
  }
};

function getSrc(elem) {
  const src = elem.getAttribute('data-echo')
  if (isNotEmpty(src)) {
    return src
  }
  return elem.getAttribute('data-original')
}

echo.detach = function () {
  if (document.removeEventListener) {
    document.removeEventListener('scroll', debounceOrThrottle);
  }
  else {
    document.detachEvent('onscroll', debounceOrThrottle);
  }
  clearTimeout(poll);
};

export default echo
