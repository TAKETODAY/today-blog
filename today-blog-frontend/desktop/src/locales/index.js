import { getCurrentLocale } from '../../../common/visitor-preferences';
import en from './en-US';
import zh from './zh-CN';

const catalogs = { 'en-US': en, 'zh-CN': zh };

export function translate(key, values = {}) {
  const messages = catalogs[getCurrentLocale()];
  const message = messages[key] ?? zh[key] ?? key;
  return message.replace(/\{(\w+)\}/g, (placeholder, name) =>
    Object.hasOwn(values, name) ? String(values[name]) : placeholder);
}

export function countMessage(key, count) {
  const category = new Intl.PluralRules(getCurrentLocale()).select(count);
  const messages = catalogs[getCurrentLocale()];
  return messages[`${key}.${category}`] ?? messages[`${key}.other`];
}
