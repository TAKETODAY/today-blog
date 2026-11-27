const { findBrowser } = require('../../browser-path');

test.each([
  ['darwin', { HOME: '/Users/test' }, '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'],
  ['win32', { PROGRAMFILES: 'C:\\Program Files' }, 'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'],
  ['linux', {}, '/usr/bin/chromium'],
  ['linux', { PATH: '/custom/bin' }, '/custom/bin/google-chrome'],
])('finds a browser on %s', (platform, env, executable) => {
  expect(findBrowser({ platform, env, exists: (value: string) => value === executable })).toBe(executable);
});

test('honors an explicit path and reports invalid overrides', () => {
  expect(findBrowser({ env: { CHROME_PATH: '/browser' }, exists: () => true })).toBe('/browser');
  expect(() => findBrowser({ env: { CHROME_PATH: '/missing' }, exists: () => false })).toThrow('CHROME_PATH');
  expect(() => findBrowser({ env: {}, exists: () => false })).toThrow('not found');
});
