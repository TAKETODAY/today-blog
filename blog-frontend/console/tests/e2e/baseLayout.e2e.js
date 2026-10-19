const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:8000';
let authenticated;
let blogger;
let pageErrors;
let unexpectedApi;

beforeEach(async () => {
  authenticated = true;
  blogger = true;
  pageErrors = [];
  unexpectedApi = [];
  await page.setViewport({ width: 1440, height: 1000 });
  await page.setRequestInterception(true);
  page.removeAllListeners('request');
  page.removeAllListeners('pageerror');
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname.startsWith('/api/')) {
      let data;
      if (url.pathname === '/api/options') data = { 'site.copyright': 'E2E Copyright' };
      else if (url.pathname === '/api/auth') data = authenticated
        ? { id: 1, name: 'E2E Blogger', blogger } : null;
      else {
        unexpectedApi.push(`${request.method()} ${url.pathname}`);
        return request.respond({ status: 500, contentType: 'application/json', body: '{}' });
      }
      return request.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(data) });
    }
    // 不访问外部 CDN、字典或真实后端。
    if (url.origin !== new URL(BASE_URL).origin) return request.abort();
    return request.continue();
  });
  await page.goto(`${BASE_URL}/#/user/login`);
  await page.waitForSelector('input');
  await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
});

afterEach(() => {
  expect(pageErrors).toEqual([]);
  expect(unexpectedApi).toEqual([]);
});

async function open(route) {
  await page.goto('about:blank');
  await page.goto(`${BASE_URL}/#${route}`);
}

test('login stays outside the admin layout', async () => {
  await open('/user/login');
  await page.waitForSelector('input');
  expect(await page.$('[aria-label="界面设置"]')).toBeNull();
  expect(await page.$('.ant-pro-sider')).toBeNull();
});

test('anonymous users cannot open the article editor', async () => {
  authenticated = false;
  await open('/articles/write');
  await page.waitForSelector('.ant-result-403');
  expect(await page.$('.CodeMirror')).toBeNull();
});

test('signed-in non-bloggers cannot open the article editor', async () => {
  blogger = false;
  await open('/articles/write');
  await page.waitForSelector('.ant-result-403');
  expect(await page.$('.CodeMirror')).toBeNull();
});

test('authenticated users can load and type in the Markdown editor', async () => {
  await open('/articles/write');
  await page.waitForSelector('.CodeMirror');
  const value = await page.evaluate(() => {
    const editor = document.querySelector('.CodeMirror').CodeMirror;
    editor.setValue('# E2E Markdown');
    return editor.getValue();
  });
  expect(value).toBe('# E2E Markdown');
  await page.reload();
  await page.waitForSelector('.CodeMirror');
});

test.each(['side', 'top', 'mix'])('settings remains available with %s navigation after reload', async layout => {
  await open('/system/account');
  await page.waitForSelector('[aria-label="界面设置"]');
  await page.click('[aria-label="界面设置"]');
  await page.waitForSelector('.ant-drawer-open');
  await page.waitForFunction(value => {
    const element = document.querySelector(`.ant-pro-setting-drawer-block-checkbox-item-${value}`);
    if (!element) return false;
    const rect = element.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && rect.left >= 0 && rect.right <= window.innerWidth;
  }, {}, layout);
  await page.click(`.ant-pro-setting-drawer-block-checkbox-item-${layout}`);
  await page.waitForFunction(value => {
    return JSON.parse(localStorage.getItem('today-blog-console-layout') || '{}').layout === value;
  }, {}, layout);
  await page.reload();
  await page.waitForSelector('[aria-label="界面设置"]');
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem('today-blog-console-layout')).layout)).toBe(layout);
});
