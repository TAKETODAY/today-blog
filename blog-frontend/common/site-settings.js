export const SITE_LOCALE = 'site.locale';
export const SITE_TIME_ZONE = 'site.timezone';
export const DEFAULT_LOCALE = 'zh-CN';
export const DEFAULT_TIME_ZONE = 'Asia/Shanghai';

export const localeOptions = [
  { value: 'zh-CN', label: '简体中文' },
  { value: 'en-US', label: 'English' },
];

export function isSiteLocale(value) {
  return localeOptions.some(option => option.value === value);
}

export function isTimeZone(value) {
  if (typeof value !== 'string' || !/^[A-Za-z][A-Za-z0-9_+/-]*$/.test(value)) {
    return false;
  }
  try {
    new Intl.DateTimeFormat('en', { timeZone: value });
    return true;
  }
  catch {
    return false;
  }
}

export function resolveSiteSettings(options = {}) {
  return {
    locale: isSiteLocale(options[SITE_LOCALE]) ? options[SITE_LOCALE] : DEFAULT_LOCALE,
    timeZone: isTimeZone(options[SITE_TIME_ZONE]) ? options[SITE_TIME_ZONE] : DEFAULT_TIME_ZONE,
  };
}

let formatter;
let formatterKey;

export function formatSiteDate(time, { locale, timeZone }, timeStyle = 'medium') {
  if (time == null || time === '') {
    return '';
  }
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const key = `${locale}|${timeZone}|${timeStyle}`;
  if (formatterKey !== key) {
    formatter = new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle, timeZone });
    formatterKey = key;
  }
  return formatter.format(date);
}
