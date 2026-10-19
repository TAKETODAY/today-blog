const fs = require('fs');
const puppeteer = require('puppeteer-core');
const findChrome = require('carlo/lib/find_chrome');

module.exports = async () => {
  const macChrome = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
  const executablePath = process.env.CHROME_PATH || (fs.existsSync(macChrome)
    ? macChrome : (await findChrome({})).executablePath);
  return puppeteer.launch({
    executablePath,
    headless: true,
    args: ['--disable-dev-shm-usage', '--no-first-run', '--no-sandbox'],
  });
};
