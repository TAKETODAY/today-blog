import React from 'react';
import { Alert, Button, Descriptions, Divider, Space, Steps } from 'antd';
import { format } from '@/utils/site-format';
import type { UpgradeRecord } from './types';
import { bytes, phases, statusLabel } from './utils';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  record: UpgradeRecord;
}

const failed = ['FAILED', 'ROLLED_BACK'];

export default function UpgradeTaskProgress({ record }: Props) {
  const t = useSiteTranslation();
  const lastPhase = phases.reduce((last, phase, index) => record.status === phase ? index : last, 0);
  const hasFailed = failed.includes(record.status);

  return <>
    <Divider orientation="left" plain>{t('升级任务', 'Upgrade task')}</Divider>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert showIcon
             type={record.status === 'SUCCESS' ? 'success' : hasFailed ? 'error' : 'info'}
             message={statusLabel(record.status)} description={record.message}/>
      <Steps size="small" responsive
             current={phases.includes(record.status) ? phases.indexOf(record.status) : lastPhase}
             status={hasFailed ? 'error' : undefined}
             items={phases.map(phase => ({ title: statusLabel(phase) }))}/>
      <Descriptions column={{ xs: 1, sm: 2 }}>
        <Descriptions.Item label={t('升级包', 'Package')}>{record.packageName}</Descriptions.Item>
        <Descriptions.Item label={t('目标版本', 'Target version')}>{record.packageVersion} / {record.packageBuild}</Descriptions.Item>
        <Descriptions.Item label={t('任务 ID', 'Task ID')}>{record.id}</Descriptions.Item>
        <Descriptions.Item
          label={t('数据库备份', 'Database backup')}>{record.backupPath || t('尚未执行', 'Not started')}{record.backupBytes ? ` / ${bytes(record.backupBytes)}` : ''}</Descriptions.Item>
        <Descriptions.Item label={t('开始时间', 'Started')}>{format(record.startedAt) || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('结束时间', 'Finished')}>{format(record.finishedAt) || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('备注', 'Notes')}>{record.note || '—'}</Descriptions.Item>
      </Descriptions>
      {record.status === 'SUCCESS' && <Button onClick={() => window.location.reload()}>{t('刷新并使用新版本', 'Reload to use the new version')}</Button>}
    </Space>
  </>;
}
