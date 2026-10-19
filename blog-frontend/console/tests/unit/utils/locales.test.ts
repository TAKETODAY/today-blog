import fs from 'fs';
import path from 'path';
import en from '@/locales/en-US';
import cn from '@/locales/zh-CN';
import tw from '@/locales/zh-TW';

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
