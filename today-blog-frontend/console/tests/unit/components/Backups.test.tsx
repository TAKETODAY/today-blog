import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import Backups from '@/pages/system/backups';
import { http } from '@/utils/http';

jest.mock('@/utils/http', () => ({ http: { get: jest.fn(), head: jest.fn(), post: jest.fn() } }));
jest.mock('@umijs/max', () => ({
  history: { push: jest.fn() },
  useIntl: require('../../site-intl').getTestIntl,
}));
jest.mock('@ant-design/pro-layout', () => ({ PageContainer: ({ children }) => <div>{children}</div> }));

describe('Backups', () => {
  const previous = globalThis.IS_REACT_ACT_ENVIRONMENT;
  let container: HTMLDivElement;
  let root = createRoot(document.createElement('div'));
  const backup = {
    id: 12, name: '更新前恢复点', status: 'AVAILABLE',
    version: '3.4', build: '123', protectedBackup: true, totalBytes: 1048576, createdAt: '2026-11-12T00:00:00Z',
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
      return { data: url.endsWith('/backups') ? [backup] : { backup, scope: '程序、数据库', restoreVerifiedAt: null } };
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
    await flushTableRequest();
    expect(container.textContent).toContain('更新前恢复点');
    expect(container.textContent).toContain('已保护');
    expect(container.textContent).not.toContain('快照');
    for (const label of ['创建备份', '恢复', '删除', '取消保护']) {
      const button = Array.from(container.querySelectorAll('button')).find(item => item.textContent === label);
      expect(button?.disabled).toBe(true);
    }
    const detail = Array.from(container.querySelectorAll('button')).find(item => item.textContent === '详情');
    await act(async () => detail?.dispatchEvent(new MouseEvent('click', { bubbles: true })));
    expect(http.get).toHaveBeenCalledWith(`/api/console/backups/${backup.id}`);
    expect(document.body.textContent).toContain('已保护，禁止删除');
    expect(http.post).not.toHaveBeenCalled();
  });

  it('reports loading failure and retries without inventing empty success', async () => {
    (http.get as jest.Mock).mockRejectedValueOnce(new Error('offline'));
    await act(async () => root.render(<Backups />));
    expect(container.textContent).toContain('备份数据加载失败');
    const retry = Array.from(container.querySelectorAll('button')).find(item => item.textContent?.replace(/\s/g, '') === '重试');
    await act(async () => retry?.dispatchEvent(new MouseEvent('click', { bubbles: true })));
    await flushTableRequest();
    expect(container.textContent).toContain('更新前恢复点');
    expect(container.textContent).not.toContain('备份数据加载失败');
  });

  it('checks availability before starting a native download', async () => {
    const click = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      expect(this.getAttribute('href')).toBe(`/api/console/backups/${backup.id}/download`);
      expect(this.hasAttribute('download')).toBe(true);
    });
    try {
      (http.head as jest.Mock).mockResolvedValueOnce({ status: 200 });
      await act(async () => root.render(<Backups />));
      await flushTableRequest();
      const download = Array.from(container.querySelectorAll('button')).find(item => item.textContent === '下载');
      expect(download).toBeDefined();
      await act(async () => download?.click());
      expect(http.head).toHaveBeenCalledWith(`/api/console/backups/${backup.id}/download`);
      expect(click).toHaveBeenCalledTimes(1);

      (http.head as jest.Mock).mockRejectedValueOnce(new Error('not found'));
      await act(async () => download?.click());
      expect(click).toHaveBeenCalledTimes(1);
      expect(container.textContent).toContain('无法下载备份');
    }
    finally {
      click.mockRestore();
    }
  });

  it('defaults to the recommended scope and submits only selected types', async () => {
    const types = ['DATABASE', 'ATTACHMENTS', 'WEBSITE', 'CONFIGURATION', 'RELEASE', 'OSS'];
    (http.get as jest.Mock).mockImplementation(async (url: string) => {
      if (url.endsWith('/capabilities')) {
        return {data: {create: true, types, offsite: true}};
      }
      return {data: []};
    });
    (http.post as jest.Mock).mockResolvedValueOnce({data: {id: '0123456789abcdef0123456789abcdef'}});

    await act(async () => root.render(<Backups />));
    const create = Array.from(container.querySelectorAll('button')).find(item => item.textContent === '创建备份');
    expect(create?.disabled).toBe(false);
    await act(async () => create?.click());
    const dialog = document.body.querySelector('[role="dialog"]')!;
    const checked = () => Array.from(dialog.querySelectorAll<HTMLInputElement>('input[type="checkbox"]:checked'))
      .map(input => input.value).filter(value => types.includes(value));
    expect(checked()).toEqual(types.slice(0, -1));
    const all = Array.from(dialog.querySelectorAll('button')).find(item => item.textContent === '全选可用类型');
    await act(async () => all?.click());
    expect(checked()).toEqual(types);

    for (const input of Array.from(dialog.querySelectorAll<HTMLInputElement>('input[type="checkbox"]:checked'))) {
      if (input.value !== 'DATABASE') {
        await act(async () => input.click());
      }
    }
    expect(checked()).toEqual(['DATABASE']);
    expect(dialog.textContent).toContain('部分备份');
    const submit = Array.from(dialog.querySelectorAll('button')).find(item => item.textContent === '创建备份');
    await act(async () => submit?.click());
    expect(http.post).toHaveBeenCalledWith('/api/console/backups', {types: ['DATABASE'], offsite: false});
    expect(http.get).toHaveBeenCalledWith('/api/console/backups/jobs');
  });
});

// ProTable debounces both the initial request and subsequent reloads.
async function flushTableRequest() {
  await act(async () => new Promise(resolve => {
    setTimeout(resolve, 100);
  }));
}
