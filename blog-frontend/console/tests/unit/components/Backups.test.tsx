import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import Backups from '@/pages/system/backups';
import { http } from '@/utils/http';

jest.mock('@/utils/http', () => ({ http: { get: jest.fn(), post: jest.fn() } }));
jest.mock('@umijs/max', () => ({ history: { push: jest.fn() } }));
jest.mock('@ant-design/pro-layout', () => ({ PageContainer: ({ children }) => <div>{children}</div> }));

describe('Backups', () => {
  const previous = globalThis.IS_REACT_ACT_ENVIRONMENT;
  let container: HTMLDivElement;
  let root = createRoot(document.createElement('div'));
  const snapshot = {
    id: '86d25e56-e760-48df-bf3d-6a75e9635026', name: '更新前恢复点', status: 'AVAILABLE',
    version: '3.4', build: '123', protectedSnapshot: true, totalBytes: 1048576, createdAt: '2026-11-12T00:00:00Z',
    operatorId: 42, manifestSha256: 'abc', sourceOperationId: null,
  };

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
    (http.get as jest.Mock).mockImplementation(async (url: string) => {
      if (url.endsWith('/capabilities')) {
        return { data: { create: false, reason: '执行器尚未接通' } };
      }
      if (url.endsWith('/operations')) {
        return { data: [] };
      }
      return { data: url.endsWith('/backups') ? [snapshot] : { snapshot, scope: '程序、数据库', restoreVerifiedAt: null } };
    });
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
  });
  afterEach(() => {
    act(() => root.unmount());
    container.remove();
  });

  it('loads records and prevents unavailable destructive actions', async () => {
    await act(async () => root.render(<Backups />));
    expect(container.textContent).toContain('更新前恢复点');
    expect(container.textContent).toContain('已保护');
    expect(container.textContent).not.toContain('快照');
    for (const label of ['创建备份', '恢复', '删除', '取消保护']) {
      const button = Array.from(container.querySelectorAll('button')).find(item => item.textContent === label);
      expect(button?.disabled).toBe(true);
    }
    const detail = Array.from(container.querySelectorAll('button')).find(item => item.textContent === '详情');
    await act(async () => detail?.dispatchEvent(new MouseEvent('click', { bubbles: true })));
    expect(http.get).toHaveBeenCalledWith(`/api/console/backups/${snapshot.id}`);
    expect(document.body.textContent).toContain('已保护，禁止删除');
    expect(http.post).not.toHaveBeenCalled();
  });

  it('reports loading failure and retries without inventing empty success', async () => {
    (http.get as jest.Mock).mockRejectedValueOnce(new Error('offline'));
    await act(async () => root.render(<Backups />));
    expect(container.textContent).toContain('备份数据加载失败');
    const retry = Array.from(container.querySelectorAll('button')).find(item => item.textContent?.replace(/\s/g, '') === '重试');
    await act(async () => retry?.dispatchEvent(new MouseEvent('click', { bubbles: true })));
    expect(container.textContent).toContain('更新前恢复点');
    expect(container.textContent).not.toContain('备份数据加载失败');
  });
});
