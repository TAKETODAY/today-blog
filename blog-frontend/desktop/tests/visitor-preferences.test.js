import {
  formatDate, getCurrentLocale, getCurrentTimeZone, getVisitorLocale,
  setDefaultSiteSettings, setVisitorPreference,
} from '../../common/visitor-preferences';

afterEach(() => {
  localStorage.clear();
  setDefaultSiteSettings();
  vi.restoreAllMocks();
});

test('negotiates browser languages, with independent explicit overrides and resets', () => {
  const languages = vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR', 'en-GB', 'zh-CN']);
  setDefaultSiteSettings({ 'site.locale': 'zh-CN', 'site.timezone': 'Europe/London' });
  expect(getCurrentLocale()).toBe('en-US');
  expect(getVisitorLocale()).toBeNull();
  expect(getCurrentTimeZone()).toBe('Europe/London');

  setVisitorPreference('locale', 'site');
  setVisitorPreference('timeZone', 'America/New_York');
  expect(getCurrentLocale()).toBe('zh-CN');
  expect(localStorage.getItem('today-blog.locale')).toBe('site');
  expect(localStorage.getItem('today-blog.timezone')).toBe('America/New_York');

  setVisitorPreference('locale', 'en-US');
  languages.mockReturnValue(['fr-FR']);
  expect(getCurrentLocale()).toBe('en-US');
  setVisitorPreference('locale', null);
  expect(getCurrentLocale()).toBe('zh-CN');
  expect(getCurrentTimeZone()).toBe('America/New_York');
  setVisitorPreference('timeZone', null);
  expect(getCurrentTimeZone()).toBe('Europe/London');
});

test('rejects invalid overrides and falls back for corrupted storage and defaults', () => {
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR']);
  setDefaultSiteSettings({ 'site.locale': 'xx', 'site.timezone': '+08:00' });
  localStorage.setItem('today-blog.locale', 'garbage');
  localStorage.setItem('today-blog.timezone', 'Mars/Olympus');
  expect(getCurrentLocale()).toBe('zh-CN');
  expect(getCurrentTimeZone()).toBe('Asia/Shanghai');
  setVisitorPreference('timeZone', 'UTC');
  setVisitorPreference('timeZone', '+05:00');
  expect(getCurrentTimeZone()).toBe('UTC');
});

test('displays daylight saving time without changing the instant', () => {
  setVisitorPreference('locale', 'en-US');
  setVisitorPreference('timeZone', 'America/New_York');
  const winter = new Date('2026-01-15T12:00:00Z');
  expect(formatDate(winter)).toContain('7:00:00');
  expect(formatDate('2026-07-15T12:00:00Z')).toContain('8:00:00');
  expect(winter.toISOString()).toBe('2026-01-15T12:00:00.000Z');
  expect(formatDate(null)).toBe('');
  expect(formatDate('invalid')).toBe('');
});
