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
      {t('数据库备份：', 'Database backup: ')}{environment?.backupReady ? t('已配置', 'Configured') : t('未就绪', 'Not ready')}
      {' · '}{t('安装盘：', 'Install disk: ')}{environment ? bytes(environment.installFreeBytes) : '—'}
      {' · '}{t('备份盘：', 'Backup disk: ')}{environment ? bytes(environment.backupFreeBytes) : '—'}
    </span>
    <Space wrap size={8} align="center">
      <span className="upgrade-secondary">{t('当前版本：', 'Current version: ')}{version.version || t('加载中', 'Loading')} / {version.build || '—'}</span>
      <Tag color={status?.enabled ? 'warning' : 'success'}>
        {status?.enabled ? (status.source === 'manual' ? t('手动维护中', 'Manual maintenance') : t('升级维护中', 'Upgrade maintenance')) : t('正常服务', 'Online')}
      </Tag>
      <Tag color={environment?.updaterReady ? 'success' : 'warning'}>
        {environment?.updaterReady ? t('升级器已就绪', 'Updater ready') : t('升级器未就绪', 'Updater not ready')}
      </Tag>
      {status?.enabled
          ? <Button danger loading={maintenance.saving} disabled={busy || !loaded}
                    onClick={maintenance.disable} icon={<StopOutlined/>}>{t('关闭维护', 'End maintenance')}</Button>
          : <Button loading={maintenance.saving} disabled={busy || !loaded}
                    onClick={maintenance.open} icon={<ToolOutlined/>}>{t('开启维护', 'Start maintenance')}</Button>}
      <Button icon={<ReloadOutlined/>} loading={refreshing} onClick={onRefresh}>{t('刷新状态', 'Refresh status')}</Button>
      <Badge dot={mappingPending} offset={[-4, 4]}>
        <Button icon={<SettingOutlined/>} onClick={onOpenConfig}>{t('升级配置', 'Upgrade configuration')}</Button>
      </Badge>
    </Space>
  </Space>;
}
