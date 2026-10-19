const { createConfig } = require('@umijs/test');

const config = createConfig({ target: 'browser', jsTransformerOpts: { jsx: 'automatic' } });

module.exports = {
  ...config,
  moduleNameMapper: {
    ...config.moduleNameMapper,
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  testEnvironment: 'jsdom',
  testEnvironmentOptions: { url: 'http://localhost:8000' },
  testMatch: ['<rootDir>/tests/unit/**/*.test.[jt]s?(x)'],
  verbose: false,
  setupFilesAfterEnv: ['<rootDir>/tests/setupTests.js'],
  globals: {
    ANT_DESIGN_PRO_ONLY_DO_NOT_USE_IN_YOUR_PRODUCTION: false,
    localStorage: null,
  },
};
