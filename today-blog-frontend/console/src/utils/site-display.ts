import { setLocale } from '@umijs/max';
import { DEFAULT_LOCALE, resolveSiteSettings } from '../../../common/site-settings';
import { setDefaultSiteSettings } from './site-format';

let options: Record<string, string> = {};
let locale = DEFAULT_LOCALE;

export function getSiteLocale() {
  return locale;
}

export function applySiteOptions(updated: Record<string, string>) {
  options = { ...options, ...updated };
  const settings = resolveSiteSettings(options);
  setDefaultSiteSettings(settings);
  // Umi compares against the current runtime locale before emitting a change.
  setLocale(settings.locale, false);
  locale = settings.locale;
  document.documentElement.lang = locale;
}
