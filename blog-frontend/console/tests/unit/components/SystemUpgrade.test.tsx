import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import SystemUpgrade from '@/pages/system/upgrade';
import { http } from '@/utils/http';

jest.mock('@/utils/http', () => ({ http: { get: jest.fn(), put: jest.fn(), post: jest.fn() } }));
jest.mock('@umijs/max', () => ({ useIntl: () => ({ locale: 'zh-CN' }) }));
jest.mock('@ant-design/pro-layout', () => ({
  PageContainer: ({ children, extra }) => <div>{extra}{children}</div>,
}));

describe('SystemUpgrade', () => {
  const previous = globalThis.IS_REACT_ACT_ENVIRONMENT;
  let container: HTMLDivElement;
  let root = createRoot(document.createElement('div'));
  let settings: { backupEnabled: boolean; backupTimeoutSeconds: number; healthTimeoutSeconds: number; note: string };

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
    settings = { backupEnabled: true, backupTimeoutSeconds: 600, healthTimeoutSeconds: 90, note: '' };
    (http.get as jest.Mock).mockImplementation(async (url: string) => {
      if (url.endsWith('/settings')) {
        return { data: { ...settings } };
      }
      if (url.endsWith('/current')) {
        return { data: { version: '3.4', build: '1' } };
      }
      if (url.endsWith('/records') || url.endsWith('/packages')) {
        return { data: [] };
      }
      if (url.endsWith('/mappings')) {
        return { data: { ready: true, roots: [], mappings: [] } };
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
    const config = Array.from(container.querySelectorAll('button')).find(element => element.textContent === '升级配置');
    await act(async () => {
      config?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    const switchButton = document.querySelector('.upgrade-config-modal [role="tabpanel"][aria-hidden="false"] [role="switch"]');
    await act(async () => {
      switchButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    const save = Array.from(document.querySelectorAll('.upgrade-config-modal button')).find(element => element.textContent === '保存默认设置');
    await act(async () => {
      save?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    expect(http.put).toHaveBeenCalledWith('/api/console/maintenance/upgrade/settings', expect.objectContaining({ backupEnabled: false }));
    expect((http.get as jest.Mock).mock.calls.filter(([url]) => url.endsWith('/settings')).length).toBe(1);
    act(() => root.unmount());
    root = createRoot(container);
    await render();
    expect(document.querySelector('.upgrade-config-modal [role="switch"]')?.getAttribute('aria-checked')).toBe('false');
  });

  it('keeps publication disabled until a package is ready', async () => {
    await render();
    expect(http.get).toHaveBeenCalledWith('/api/console/maintenance/upgrade/packages');
    expect(Array.from(container.querySelectorAll('button')).find(element => element.textContent === '上传升级包')?.disabled).toBe(false);
    expect(Array.from(container.querySelectorAll('button')).some(element => element.textContent?.replace(/\s/g, '') === '升级')).toBe(false);
  });
});
