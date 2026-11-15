import React from 'react';
import { Badge, Button, Space, Tag } from 'antd';
import { ReloadOutlined, SettingOutlined, StopOutlined, ToolOutlined } from '@ant-design/icons';
import type { Environment, VersionInfo } from './types';
import type { MaintenanceController } from './useMaintenance';
import { bytes } from './utils';

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
  const { status } = maintenance;

  return <Space direction="vertical" align="end" size={4}>
    <span className="upgrade-secondary">
      数据库备份：{environment?.backupReady ? '已配置' : '未就绪'} · 安装盘：{environment ? bytes(environment.installFreeBytes) : '—'} · 备份盘：{environment ? bytes(environment.backupFreeBytes) : '—'}
    </span>
    <Space wrap size={8} align="center">
      <span className="upgrade-secondary">当前版本：{version.version} / {version.build || '—'}</span>
      <Tag color={status?.enabled ? 'warning' : 'success'}>
        {status?.enabled ? (status.source === 'manual' ? '手动维护中' : '升级维护中') : '正常服务'}
      </Tag>
      <Tag color={environment?.updaterReady ? 'success' : 'warning'}>
        升级器{environment?.updaterReady ? '已就绪' : '未就绪'}
      </Tag>
      {status?.enabled
          ? <Button danger loading={maintenance.saving} disabled={busy || !loaded}
                    onClick={maintenance.disable} icon={<StopOutlined/>}>关闭维护</Button>
          : <Button loading={maintenance.saving} disabled={busy || !loaded}
                    onClick={maintenance.open} icon={<ToolOutlined/>}>开启维护</Button>}
      <Button icon={<ReloadOutlined/>} loading={refreshing} onClick={onRefresh}>刷新状态</Button>
      <Badge dot={mappingPending} offset={[-4, 4]}>
        <Button icon={<SettingOutlined/>} onClick={onOpenConfig}>升级配置</Button>
      </Badge>
    </Space>
  </Space>;
}
