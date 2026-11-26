import { getIntl } from '@umijs/max';

// 在调用时读取当前语言，供组件及异步操作共用。
export function articleMessage(key: string, values?: Record<string, string | number>) {
  return getIntl().formatMessage({ id: `articles.${key}` }, values);
}
