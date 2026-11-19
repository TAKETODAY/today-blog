import { useCallback } from 'react';
import { useIntl } from '@umijs/max';

export function useSiteTranslation() {
  const { locale } = useIntl();
  return useCallback((zh: string, en: string) => locale === 'en-US' ? en : zh, [locale]);
}
