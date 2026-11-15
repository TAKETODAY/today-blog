import type { Options } from './types';

export const terminal = ['SUCCESS', 'FAILED', 'ROLLED_BACK'];
export const taskKey = 'today-blog-upgrade-task';
export const phases = ['VALIDATING', 'INSTALLING', 'BACKING_UP', 'RESTARTING', 'HEALTH_CHECKING', 'SUCCESS'];
export const statusLabels: Record<string, string> = {
  STARTING: '已提交', VALIDATING: '校验升级包', INSTALLING: '准备版本',
  BACKING_UP: '数据库备份', RESTARTING: '切换并重启', HEALTH_CHECKING: '健康检查',
  SUCCESS: '升级成功', FAILED: '升级失败', ROLLING_BACK: '正在恢复旧程序', ROLLED_BACK: '升级失败，已恢复旧版本',
  READY: '待升级', CONSUMED: '已使用',
};

export function parseOptions(data?: Partial<Options>): Options {
  return {
    backupEnabled: data?.backupEnabled ?? true,
    backupTimeoutSeconds: data?.backupTimeoutSeconds ?? 600,
    healthTimeoutSeconds: data?.healthTimeoutSeconds ?? 90,
    note: data?.note || '',
  };
}

export function requestOptions(options: Options): Options {
  return { ...options, note: options.note || '' };
}

export function bytes(value?: number) {
  const size = Number(value);
  if (!Number.isFinite(size) || size < 0) {
    return '—';
  }
  if (size >= 1024 ** 3) {
    return `${(size / 1024 ** 3).toFixed(2)} GiB`;
  }
  return `${(size / 1024 ** 2).toFixed(1)} MiB`;
}
