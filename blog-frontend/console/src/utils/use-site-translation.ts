import { useCallback } from 'react';
import { useIntl } from '@umijs/max';
import type { MessageValues } from './site-i18n';

export function useSiteTranslation() {
  const intl = useIntl();
  return useCallback((id: string, values?: MessageValues) => intl.formatMessage({ id }, values), [intl]);
}
