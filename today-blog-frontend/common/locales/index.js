import { getCurrentLocale } from '../visitor-preferences';
import en from './en-US';
import zh from './zh-CN';

export function translate(key) {
  const messages = getCurrentLocale() === 'en-US' ? en : zh;
  return messages[key] ?? key;
}
