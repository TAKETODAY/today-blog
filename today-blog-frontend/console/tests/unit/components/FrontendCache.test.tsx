import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import FrontendCache from '@/pages/system/frontend-cache';
import { http } from '@/utils';

jest.mock('@/utils', () => ({ http: { get: jest.fn(), put: jest.fn() } }));
jest.mock('@umijs/max', () => ({ useIntl: require('../../site-intl').getTestIntl }));
jest.mock('@ant-design/pro-layout', () => ({ PageContainer: ({ children }) => <div>{children}</div> }));

describe('FrontendCache', () => {
  const endpoint = '/api/console/frontend-cache';
  const actEnvironment = globalThis.IS_REACT_ACT_ENVIRONMENT;
  const matchMedia = window.matchMedia;
  let container: HTMLDivElement;
  let root = createRoot(document.createElement('div'));

  beforeAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = true;
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      writable: true,
      value: jest.fn(() => ({ matches: false, addListener: jest.fn(), removeListener: jest.fn() })),
    });
  });

  afterAll(() => {
    globalThis.IS_REACT_ACT_ENVIRONMENT = actEnvironment;
    window.matchMedia = matchMedia;
  });

  beforeEach(() => {
    jest.clearAllMocks();
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
  });

  afterEach(() => {
    act(() => {
      root.unmount();
    });
    container.remove();
  });

  it('saves the selected preset for one resource without changing the other choices', async () => {
    const initial = { embedded: true, presets: { 'desktop.index': 'no-store' }, effective: { upload: 'no-cache' } };
    (http.get as jest.Mock).mockResolvedValue({ data: initial });
    (http.put as jest.Mock).mockResolvedValue({ data: { ...initial, effective: { upload: 'public, max-age=300' } } });
    await act(async () => {
      root.render(<FrontendCache/>);
    });
    expect(container.querySelectorAll('[role="combobox"]')).toHaveLength(7);
    expect(container.textContent).toContain('no-cache');
    const upload = container.querySelector('#upload');
    await act(async () => {
      upload?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    });
    const fiveMinutes = document.querySelector('[title="缓存 5 分钟"]');
    expect(fiveMinutes).not.toBeNull();
    await act(async () => {
      fiveMinutes?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    });
    await act(async () => {
      container.querySelector('form')?.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    });
    expect(http.put).toHaveBeenCalledWith(endpoint, {
      'console.index': 'inherit', 'console.assets': 'inherit', 'console.hashed-assets': 'inherit',
      'desktop.index': 'no-store', 'desktop.assets': 'inherit', 'desktop.hashed-assets': 'inherit',
      upload: '5m',
    });
    expect(container.textContent).toContain('public, max-age=300');
  });

  it('shows NGINX guidance instead of editable settings in NGINX mode', async () => {
    (http.get as jest.Mock).mockResolvedValue({ data: { embedded: false, presets: {}, effective: {} } });
    await act(async () => {
      root.render(<FrontendCache/>);
    });
    expect(container.textContent).toContain('当前由 NGINX 托管前端');
    expect(container.querySelector('form')).toBeNull();
  });
});
