import React from 'react';
import { Badge, Button, Space, Tag } from 'antd';
import { ReloadOutlined, SettingOutlined, StopOutlined, ToolOutlined } from '@ant-design/icons';
import type { Environment, VersionInfo } from './types';
import type { MaintenanceController } from './useMaintenance';
import { bytes } from './utils';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  version: VersionInfo;
  environment?: Environment;
  maintenance: MaintenanceController;
  busy: boolean;
  loaded: boolean;
  refreshing: boolean;
  mappingPending: boolean;
  onRefresh: () => void;
  onOpenConfig: () => void;
}

export default function UpgradeHeaderExtra({
  version, environment, maintenance, busy, loaded, refreshing, mappingPending, onRefresh, onOpenConfig,
}: Props) {
  const t = useSiteTranslation();
  const { status } = maintenance;

  return <Space direction="vertical" align="end" size={4}>
    <span className="upgrade-secondary">
      {t('upgrade.databaseBackupPrefix')}{environment?.backupReady ? t('upgrade.configured') : t('upgrade.notReady')}
      {' · '}{t('upgrade.installDisk')}{environment ? bytes(environment.installFreeBytes) : '—'}
      {' · '}{t('upgrade.backupDisk')}{environment ? bytes(environment.backupFreeBytes) : '—'}
    </span>
    <Space wrap size={8} align="center">
      <span className="upgrade-secondary">{t('upgrade.currentVersion')}{version.version || t('upgrade.loading')} / {version.build || '—'}</span>
      <Tag color={status?.enabled ? 'warning' : 'success'}>
        {status?.enabled ? (status.source === 'manual' ? t('upgrade.manualMaintenance') : t('upgrade.upgradeMaintenance')) : t('upgrade.online')}
      </Tag>
      <Tag color={environment?.updaterReady ? 'success' : 'warning'}>
        {environment?.updaterReady ? t('upgrade.updaterReady') : t('upgrade.updaterNotReady')}
      </Tag>
      {status?.enabled
          ? <Button danger loading={maintenance.saving} disabled={busy || !loaded}
                    onClick={maintenance.disable} icon={<StopOutlined/>}>{t('upgrade.endMaintenance')}</Button>
          : <Button loading={maintenance.saving} disabled={busy || !loaded}
                    onClick={maintenance.open} icon={<ToolOutlined/>}>{t('upgrade.startMaintenance')}</Button>}
      <Button icon={<ReloadOutlined/>} loading={refreshing} onClick={onRefresh}>{t('upgrade.refreshStatus')}</Button>
      <Badge dot={mappingPending} offset={[-4, 4]}>
        <Button icon={<SettingOutlined/>} onClick={onOpenConfig}>{t('upgrade.upgradeConfiguration')}</Button>
      </Badge>
    </Space>
  </Space>;
}
