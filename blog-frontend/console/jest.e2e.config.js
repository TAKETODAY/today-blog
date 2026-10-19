const config = require('./jest.config');

module.exports = {
  ...config,
  testEnvironment: './tests/PuppeteerEnvironment',
  testMatch: ['**/src/e2e/**/*.e2e.js'],
};
