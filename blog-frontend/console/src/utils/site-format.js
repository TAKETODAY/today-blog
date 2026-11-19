import { formatSiteDate, resolveSiteSettings } from '../../../common/site-settings';

let settings = resolveSiteSettings();

export function setDefaultSiteSettings(value) {
  settings = value;
}

export function siteText(zh, en) {
  return settings.locale === 'en-US' ? en : zh;
}

export function format(time, timeStyle = 'medium') {
  return formatSiteDate(time, settings, timeStyle);
}
