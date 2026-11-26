jest.mock('@umijs/max', () => ({ defineConfig: (config: unknown) => config }));

import config from '../../../config/config';

describe('browser environment definitions', () => {
  it('defines the legacy Pro globals instead of only declaring their types', () => {
    expect(config.define).toHaveProperty('REACT_APP_ENV');
    expect(config.define.REACT_APP_ENV).toBe(process.env.REACT_APP_ENV || false);
    expect(config.define.ANT_DESIGN_PRO_ONLY_DO_NOT_USE_IN_YOUR_PRODUCTION).toBe(false);
  });
});
