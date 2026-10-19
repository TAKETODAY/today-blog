const config = require('./jest.config');

module.exports = {
  ...config,
  testEnvironment: './tests/PuppeteerEnvironment',
  testMatch: ['<rootDir>/tests/e2e/**/*.e2e.js'],
};
