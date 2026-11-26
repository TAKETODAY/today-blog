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
    <Divider orientation="left" plain>{t('upgrade.upgradeTask')}</Divider>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert showIcon
             type={record.status === 'SUCCESS' ? 'success' : hasFailed ? 'error' : 'info'}
             message={statusLabel(record.status)} description={record.message}/>
      <Steps size="small" responsive
             current={phases.includes(record.status) ? phases.indexOf(record.status) : lastPhase}
             status={hasFailed ? 'error' : undefined}
             items={phases.map(phase => ({ title: statusLabel(phase) }))}/>
      <Descriptions column={{ xs: 1, sm: 2 }}>
        <Descriptions.Item label={t('upgrade.package')}>{record.packageName}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.targetVersion')}>{record.packageVersion} / {record.packageBuild}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.taskID')}>{record.id}</Descriptions.Item>
        <Descriptions.Item
          label={t('upgrade.databaseBackup')}>{record.backupPath || t('upgrade.notStarted')}{record.backupBytes ? ` / ${bytes(record.backupBytes)}` : ''}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.started')}>{format(record.startedAt) || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.finished')}>{format(record.finishedAt) || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.notes')}>{record.note || '—'}</Descriptions.Item>
      </Descriptions>
      {record.status === 'SUCCESS' && <Button onClick={() => window.location.reload()}>{t('upgrade.reloadToUseTheNew')}</Button>}
    </Space>
  </>;
}
