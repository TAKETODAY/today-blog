import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import RequireLogin from '@/wrappers/RequireLogin';
import Forbidden from '@/pages/403';
import { useAccess } from '@umijs/max';
import { showLoginDialog } from '@/utils/error-handler';

jest.mock('@umijs/max', () => ({
  useAccess: jest.fn(),
  Outlet: () => <div>protected content</div>,
}));
jest.mock('@/utils/error-handler', () => ({ showLoginDialog: jest.fn() }));

describe('RequireLogin', () => {
  const actEnvironment = globalThis.IS_REACT_ACT_ENVIRONMENT;
  beforeAll(() => { globalThis.IS_REACT_ACT_ENVIRONMENT = true; });
  afterAll(() => { globalThis.IS_REACT_ACT_ENVIRONMENT = actEnvironment; });
  beforeEach(() => jest.clearAllMocks());

  it('prompts for login without exposing protected content', () => {
    (useAccess as jest.Mock).mockReturnValue({ isLoggedIn: false });
    const container = document.createElement('div');
    const root = createRoot(container);
    try {
      act(() => root.render(<RequireLogin />));
      expect(showLoginDialog).toHaveBeenCalledTimes(1);
      expect(container.textContent).toContain('403');
      act(() => root.render(<RequireLogin />));
      expect(showLoginDialog).toHaveBeenCalledTimes(1);
    } finally {
      act(() => root.unmount());
    }
  });

  it('prompts when the layout renders 403 without mounting the route wrapper', () => {
    const container = document.createElement('div');
    const root = createRoot(container);
    try {
      act(() => root.render(<Forbidden />));
      expect(showLoginDialog).toHaveBeenCalledTimes(1);
      expect(container.textContent).toContain('403');
    } finally {
      act(() => root.unmount());
    }
  });

  it('renders protected content without prompting an authorized user', () => {
    (useAccess as jest.Mock).mockReturnValue({ isLoggedIn: true });
    const container = document.createElement('div');
    const root = createRoot(container);
    try {
      act(() => root.render(<RequireLogin />));
      expect(container.textContent).toBe('protected content');
      expect(showLoginDialog).not.toHaveBeenCalled();
    } finally {
      act(() => root.unmount());
    }
  });
});
