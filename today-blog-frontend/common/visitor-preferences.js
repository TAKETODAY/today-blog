import { formatSiteDate, isSiteLocale, isTimeZone, resolveSiteSettings } from './site-settings';

const keys = { locale: 'today-blog.locale', timeZone: 'today-blog.timezone' };
const listeners = new Set();
const values = {};
let site = resolveSiteSettings();

function read(name) {
  if (Object.hasOwn(values, name)) {
    return values[name];
  }
  try {
    return window.localStorage.getItem(keys[name]);
  }
  catch {
    return null;
  }
}

export function getVisitorLocale() {
  const value = read('locale');
  return value === 'site' || isSiteLocale(value) ? value : null;
}

export function getVisitorTimeZone() {
  const value = read('timeZone');
  return isTimeZone(value) ? value : null;
}

export function browserLocale(languages) {
  for (const tag of languages) {
    switch (tag.toLowerCase().split('-')[0]) {
      case 'en':
        return 'en-US';
      case 'zh':
        return 'zh-CN';
    }
  }
  return null;
}

export function getCurrentLocale() {
  const selected = getVisitorLocale();
  if (selected === 'site') {
    return site.locale;
  }
  const languages = globalThis.navigator?.languages;
  return selected || browserLocale(languages?.length ? languages : [globalThis.navigator?.language || '']) || site.locale;
}

export function getCurrentTimeZone() {
  return getVisitorTimeZone() || site.timeZone;
}

export function setDefaultSiteSettings(options) {
  site = resolveSiteSettings(options);
  notify();
}

export function setVisitorPreference(name, value) {
  if (name !== 'locale' && name !== 'timeZone') {
    return;
  }
  const valid = name === 'locale' ? value === 'site' || isSiteLocale(value) : isTimeZone(value);
  if (value != null && !valid) {
    return;
  }
  try {
    if (value == null) {
      window.localStorage.removeItem(keys[name]);
    }
    else {
      window.localStorage.setItem(keys[name], value);
    }
    delete values[name];
  }
  catch {
    values[name] = value;
  }
  notify();
}

function notify() {
  listeners.forEach(listener => listener());
}

export function subscribePreferences(listener) {
  listeners.add(listener);
  window.addEventListener('languagechange', listener);
  window.addEventListener('storage', listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener('languagechange', listener);
    window.removeEventListener('storage', listener);
  };
}

export function preferencesSnapshot() {
  return [getVisitorLocale(), getVisitorTimeZone(), getCurrentLocale(), getCurrentTimeZone()].join('|');
}

export function formatDate(time) {
  return formatSiteDate(time, { locale: getCurrentLocale(), timeZone: getCurrentTimeZone() });
}
