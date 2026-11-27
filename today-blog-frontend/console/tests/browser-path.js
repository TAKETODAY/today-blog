const fs = require('fs');
const path = require('path');

function findBrowser({ platform = process.platform, env = process.env, exists = fs.existsSync } = {}) {
  if (env.CHROME_PATH) {
    if (!exists(env.CHROME_PATH)) throw new Error(`CHROME_PATH does not exist: ${env.CHROME_PATH}`);
    return env.CHROME_PATH;
  }
  const candidates = [];
  if (platform === 'darwin') {
    for (const root of ['/Applications', path.join(env.HOME || '', 'Applications')]) {
      candidates.push(
        `${root}/Google Chrome.app/Contents/MacOS/Google Chrome`,
        `${root}/Chromium.app/Contents/MacOS/Chromium`,
        `${root}/Microsoft Edge.app/Contents/MacOS/Microsoft Edge`,
      );
    }
  }
  if (platform === 'win32') {
    for (const root of [env.PROGRAMFILES, env['PROGRAMFILES(X86)'], env.LOCALAPPDATA].filter(Boolean)) {
      candidates.push(path.win32.join(root, 'Google/Chrome/Application/chrome.exe'),
        path.win32.join(root, 'Microsoft/Edge/Application/msedge.exe'));
    }
  }
  const names = platform === 'win32' ? ['chrome.exe', 'msedge.exe', 'chromium.exe']
    : ['google-chrome', 'google-chrome-stable', 'chromium', 'chromium-browser', 'microsoft-edge'];
  const paths = (env.PATH || '').split(platform === 'win32' ? ';' : ':').filter(Boolean);
  if (platform === 'linux') paths.push('/usr/bin', '/opt/google/chrome', '/snap/bin');
  const join = platform === 'win32' ? path.win32.join : path.posix.join;
  for (const directory of paths) for (const name of names) candidates.push(join(directory, name));
  const browser = candidates.find(exists);
  if (!browser) throw new Error('Chrome/Chromium/Edge not found. Install a browser or set CHROME_PATH to its executable.');
  return browser;
}

module.exports = { findBrowser };
