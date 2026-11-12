import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import SystemUpgrade from '@/pages/system/upgrade';
import { http } from '@/utils/http';

jest.mock('@/utils/http', () => ({ http: { get: jest.fn(), put: jest.fn(), post: jest.fn() } }));
jest.mock('@ant-design/pro-layout', () => ({
  PageContainer: ({ children }) => <div>{children}</div>,
}));

describe('SystemUpgrade', () => {
  const previous = globalThis.IS_REACT_ACT_ENVIRONMENT;
  let container: HTMLDivElement;
  let root = createRoot(document.createElement('div'));
  let settings: Record<string, string>;

  beforeAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = true;
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: jest.fn().mockImplementation(() => ({ matches: false, addListener: jest.fn(), removeListener: jest.fn() })),
    });
  });

  afterAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = previous;
  });

  beforeEach(() => {
    jest.clearAllMocks();
    settings = { backupEnabled: 'true', backupTimeoutSeconds: '600', healthTimeoutSeconds: '90', note: '' };
    (http.get as jest.Mock).mockImplementation(async (url: string) => {
      if (url.endsWith('/settings')) {
        return { data: { ...settings } };
      }
      if (url.endsWith('/current')) {
        return { data: { version: '3.4', build: '1' } };
      }
      if (url.endsWith('/history')) {
        return { data: [] };
      }
      return { data: { updaterReady: true, backupReady: true, installFreeBytes: 10 * 1024 ** 3, backupFreeBytes: 10 * 1024 ** 3 } };
    });
    (http.put as jest.Mock).mockImplementation(async (_, values) => {
      settings = values;
      return { data: values };
    });
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
  });

  afterEach(() => {
    act(() => root.unmount());
    container.remove();
  });

  async function render() {
    await act(async () => {
      root.render(<SystemUpgrade />);
    });
  }

  it('loads environment and persists defaults with a read-back', async () => {
    await render();
    expect(container.textContent).toContain('3.4 / 1');
    const tab = Array.from(container.querySelectorAll('[role="tab"]')).find(element => element.textContent === '更新设置');
    await act(async () => {
      tab?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    const switchButton = container.querySelector('[role="tabpanel"][aria-hidden="false"] [role="switch"]');
    await act(async () => {
      switchButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    const save = Array.from(container.querySelectorAll('button')).find(element => element.textContent === '保存默认设置');
    await act(async () => {
      save?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    expect(http.put).toHaveBeenCalledWith('/api/console/upgrade/settings', expect.objectContaining({ backupEnabled: 'false' }));
    expect((http.get as jest.Mock).mock.calls.filter(([url]) => url.endsWith('/settings')).length).toBe(2);
    act(() => root.unmount());
    root = createRoot(container);
    await render();
    expect(container.querySelector('[role="switch"]')?.getAttribute('aria-checked')).toBe('false');
  });

  it('keeps publication disabled until a package is ready', async () => {
    await render();
    const start = Array.from(container.querySelectorAll('button')).find(element => element.textContent === '开始更新');
    expect(start?.disabled).toBe(true);
  });
});
