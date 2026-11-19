import type { Options } from './types';
import { siteText } from '@/utils/site-format';

export const terminal = ['SUCCESS', 'FAILED', 'ROLLED_BACK'];
export const taskKey = 'today-blog-upgrade-task';
export const phases = ['VALIDATING', 'INSTALLING', 'BACKING_UP', 'RESTARTING', 'HEALTH_CHECKING', 'SUCCESS'];
const statusLabels: Record<string, [string, string]> = {
  STARTING: ['已提交', 'Submitted'], VALIDATING: ['校验升级包', 'Validating package'],
  INSTALLING: ['准备版本', 'Preparing release'], BACKING_UP: ['数据库备份', 'Backing up database'],
  RESTARTING: ['切换并重启', 'Switching and restarting'], HEALTH_CHECKING: ['健康检查', 'Checking health'],
  SUCCESS: ['升级成功', 'Upgrade succeeded'], FAILED: ['升级失败', 'Upgrade failed'],
  ROLLING_BACK: ['正在恢复旧程序', 'Rolling back'], ROLLED_BACK: ['升级失败，已恢复旧版本', 'Upgrade failed and was rolled back'],
  READY: ['待升级', 'Ready'], CONSUMED: ['已使用', 'Used'],
};

export function statusLabel(status: string) {
  const label = statusLabels[status];
  return Array.isArray(label) ? siteText(...label) : status;
}

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
