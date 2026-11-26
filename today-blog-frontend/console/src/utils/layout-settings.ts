import type { Settings } from '@ant-design/pro-layout';
import defaults from '../../config/defaultSettings';

const storageKey = 'today-blog-console-layout';

export function normalizeLayoutSettings(value: Partial<Settings> = {}): Partial<Settings> {
  return {
    navTheme: value.navTheme === 'dark' ? 'dark' : defaults.navTheme,
    layout: ['side', 'top', 'mix'].includes(value.layout || '') ? value.layout : defaults.layout,
    contentWidth: value.contentWidth === 'Fixed' ? 'Fixed' : 'Fluid',
    fixedHeader: typeof value.fixedHeader === 'boolean' ? value.fixedHeader : defaults.fixedHeader,
    fixSiderbar: typeof value.fixSiderbar === 'boolean' ? value.fixSiderbar : defaults.fixSiderbar,
    colorWeak: value.colorWeak === true,
    primaryColor: defaults.primaryColor,
  };
}

export function loadLayoutSettings(): Partial<Settings> {
  try {
    return normalizeLayoutSettings(JSON.parse(localStorage.getItem(storageKey) || '{}') || {});
  }
  catch {
    return normalizeLayoutSettings();
  }
}

export function saveLayoutSettings(value: Partial<Settings>): Partial<Settings> {
  const settings = normalizeLayoutSettings(value);
  localStorage.setItem(storageKey, JSON.stringify(settings));
  return settings;
}
