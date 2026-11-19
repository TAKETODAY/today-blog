import { getIntl } from '@umijs/max';
import { getDisplayLocale } from './site-format';

export type MessageValues = Record<string, string | number>;

export function siteText(id: string, values?: MessageValues) {
  return getIntl(getDisplayLocale()).formatMessage({ id }, values);
}
