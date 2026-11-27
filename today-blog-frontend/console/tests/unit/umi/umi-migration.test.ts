import routes from '../../../config/routes';
import access from '@/access';

describe('Umi 4 migration', () => {
  it('keeps article editor routes and uses a React Router 6 catch-all', () => {
    const articles = routes.find(route => route.path === '/articles');
    expect(articles?.layout).toBe(false);
    expect(articles?.routes?.map(route => route.path)).toEqual([
      '/articles/write',
      '/articles/write-rich-text',
      '/articles/:id/modify',
      '/articles/:id/modify-rich-text',
    ]);
    expect(routes[routes.length - 1]).toEqual({
      path: '*', redirect: '/not-found',
    });
  });

  it('allows anonymous login but restricts console pages to bloggers', () => {
    expect(access({})).toEqual({ none: true, isLoggedIn: false });
    expect(access({ currentUser: { blogger: false } as API.CurrentUser }).isLoggedIn).toBe(false);
    expect(access({ currentUser: { blogger: true } as API.CurrentUser }).isLoggedIn).toBe(true);
    expect(routes.find(route => route.path === '/user/login')).toMatchObject({
      layout: false, access: 'none',
    });
  });
});
