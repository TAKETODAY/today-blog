import en from '@/locales/en-US/articles';
import cn from '@/locales/zh-CN/articles';
import tw from '@/locales/zh-TW/articles';
import { articleMessage } from '@/utils/article-i18n';

let mockLocale = 'en-US';
jest.mock('@umijs/max', () => ({
  getIntl: () => ({
    formatMessage: ({ id }: { id: string }, values: Record<string, string> = {}) => {
      const dictionaries = {
        'en-US': require('@/locales/en-US/articles').default,
        'zh-CN': require('@/locales/zh-CN/articles').default,
        'zh-TW': require('@/locales/zh-TW/articles').default,
      };
      return Object.entries(values).reduce((text, [key, value]) => text.replace(`{${key}}`, value), dictionaries[mockLocale][id]);
    },
  }),
}));

test('article dictionaries have identical keys', () => {
  expect(Object.keys(en).sort()).toEqual(Object.keys(cn).sort());
  expect(Object.keys(tw).sort()).toEqual(Object.keys(cn).sort());
});

test('reads the current language on each call and preserves user-provided titles', () => {
  mockLocale = 'en-US';
  expect(articleMessage('saveArticle')).toBe('Save article');
  expect(articleMessage('deletingName', { name: '我的文章' })).toBe('Deleting: 我的文章');
  mockLocale = 'zh-TW';
  expect(articleMessage('saveArticle')).toBe('儲存文章');
  mockLocale = 'zh-CN';
  expect(articleMessage('saveArticle')).toBe('保存文章');
});
