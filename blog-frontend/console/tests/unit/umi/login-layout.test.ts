import fs from 'fs';
import path from 'path';

describe('login route layout', () => {
  it('keeps login outside the generated main layout and authenticated parents', () => {
    const source = fs.readFileSync(path.resolve(__dirname, '../../../src/.umi/core/route.tsx'), 'utf8');
    const routes = JSON.parse(source.split('const routes = ')[1].split('\n')[0].replace(/\s+as const;$/, ''));
    const login: any = Object.values(routes).find((route: any) => route.path === '/user/login');
    expect(login).toBeDefined();
    expect(login.layout).toBe(false);
    let current = login;
    while (current) {
      expect(current.id).not.toBe('ant-design-pro-layout');
      expect(current.access).not.toBe('isLoggedIn');
      current = routes[current.parentId];
    }
    const users: any = Object.values(routes).find((route: any) => route.path === '/user/list');
    expect(routes[users.parentId].parentId).toBe('ant-design-pro-layout');
  });
});
