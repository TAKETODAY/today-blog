const config = require('./jest.config');

module.exports = {
  ...config,
  testEnvironment: './tests/PuppeteerEnvironment',
  setupFilesAfterEnv: [],
  testTimeout: 30000,
  testMatch: ['<rootDir>/tests/e2e/**/*.e2e.js'],
};
