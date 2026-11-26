import type { Options } from './types';
import { siteText } from '@/utils/site-i18n';

export const terminal = ['SUCCESS', 'FAILED', 'ROLLED_BACK'];
export const taskKey = 'today-blog-upgrade-task';
export const phases = ['VALIDATING', 'INSTALLING', 'BACKING_UP', 'RESTARTING', 'HEALTH_CHECKING', 'SUCCESS'];
const statusLabels: Record<string, string> = {
  STARTING: 'upgrade.status.starting', VALIDATING: 'upgrade.status.validating',
  INSTALLING: 'upgrade.status.installing', BACKING_UP: 'upgrade.status.backingUp',
  RESTARTING: 'upgrade.status.restarting', HEALTH_CHECKING: 'upgrade.status.healthChecking',
  SUCCESS: 'upgrade.status.success', FAILED: 'upgrade.status.failed',
  ROLLING_BACK: 'upgrade.status.rollingBack', ROLLED_BACK: 'upgrade.status.rolledBack',
  READY: 'upgrade.status.ready', CONSUMED: 'upgrade.status.consumed',
};

export function statusLabel(status: string) {
  const label = statusLabels[status];
  return Object.hasOwn(statusLabels, status) ? siteText(label) : status;
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
