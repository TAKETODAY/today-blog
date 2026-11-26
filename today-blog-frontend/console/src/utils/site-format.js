import { formatSiteDate, resolveSiteSettings } from '../../../common/site-settings';

let settings = resolveSiteSettings();

export function setDefaultSiteSettings(value) {
  settings = value;
}

export function getDisplayLocale() {
  return settings.locale;
}

export function format(time, timeStyle = 'medium') {
  return formatSiteDate(time, settings, timeStyle);
}
