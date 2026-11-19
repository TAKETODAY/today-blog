import fs from 'fs';
import path from 'path';
import en from '@/locales/en-US';
import cn from '@/locales/zh-CN';
import tw from '@/locales/zh-TW';
import { getTestIntl } from '../../site-intl';

test('ships only English, Simplified Chinese and Traditional Chinese', () => {
  const files = fs.readdirSync(path.resolve(__dirname, '../../../src/locales'));
  expect(files.filter(file => file.endsWith('.ts')).sort()).toEqual(['en-US.ts', 'zh-CN.ts', 'zh-TW.ts']);
});

test('translates all Chinese message keys in English and Traditional Chinese', () => {
  for (const key of Object.keys(cn)) {
    expect(en).toHaveProperty([key]);
    expect(tw).toHaveProperty([key]);
  }
});

test('formats complete messages with values in both site languages', () => {
  const id = 'upgrade.historyParameters';
  const values = { backup: 'enabled', backupTimeout: 600, healthTimeout: 90 };
  expect(getTestIntl('zh-CN').formatMessage({ id }, values)).toBe('备份：开启；备份超时：600 秒；健康检查：90 秒');
  expect(getTestIntl('en-US').formatMessage({ id }, values)).toBe('Backup: enabled; backup timeout: 600s; health check: 90s');
  expect(getTestIntl('en-US').formatMessage({ id }, { ...values, backup: 'disabled' })).toContain('Backup: disabled;');
});
