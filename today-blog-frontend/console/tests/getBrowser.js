const puppeteer = require('puppeteer-core');
const { findBrowser } = require('./browser-path');

module.exports = async () => {
  return puppeteer.launch({
    executablePath: findBrowser(),
    headless: true,
    args: [
      '--no-first-run',
      ...(process.platform === 'linux' ? ['--disable-dev-shm-usage'] : []),
      ...(process.env.E2E_NO_SANDBOX === '1' ? ['--no-sandbox'] : []),
    ],
  });
};
