import en from '@/locales/en-US';
import zh from '@/locales/zh-CN';

// Use Umi's message formatter without booting the application in component tests.
jest.mock('@/.umi/core/plugin', () => ({}));
const { createIntl } = require('@/.umi/plugin-locale/localeExports') as typeof import('@/.umi/plugin-locale/localeExports');

const intl = {
  'en-US': createIntl({ locale: 'en-US', messages: en }),
  'zh-CN': createIntl({ locale: 'zh-CN', messages: zh }),
};

export function getTestIntl(locale: keyof typeof intl = 'zh-CN') {
  return intl[locale];
}
