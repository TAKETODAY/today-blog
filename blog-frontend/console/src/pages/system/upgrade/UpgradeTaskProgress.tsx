import React from 'react';
import { Alert, Button, Descriptions, Divider, Space, Steps } from 'antd';
import type { UpgradeRecord } from './types';
import { bytes, phases, statusLabels } from './utils';

interface Props {
  record: UpgradeRecord;
}

const failed = ['FAILED', 'ROLLED_BACK'];

export default function UpgradeTaskProgress({ record }: Props) {
  const lastPhase = phases.reduce((last, phase, index) => record.status === phase ? index : last, 0);
  const hasFailed = failed.includes(record.status);

  return <>
    <Divider orientation="left" plain>升级任务</Divider>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert showIcon
             type={record.status === 'SUCCESS' ? 'success' : hasFailed ? 'error' : 'info'}
             message={statusLabels[record.status] || record.status} description={record.message}/>
      <Steps size="small" responsive
             current={phases.includes(record.status) ? phases.indexOf(record.status) : lastPhase}
             status={hasFailed ? 'error' : undefined}
             items={phases.map(phase => ({ title: statusLabels[phase] }))}/>
      <Descriptions column={{ xs: 1, sm: 2 }}>
        <Descriptions.Item label="升级包">{record.packageName}</Descriptions.Item>
        <Descriptions.Item label="目标版本">{record.packageVersion} / {record.packageBuild}</Descriptions.Item>
        <Descriptions.Item label="任务 ID">{record.id}</Descriptions.Item>
        <Descriptions.Item
          label="数据库备份">{record.backupPath || '尚未执行'}{record.backupBytes ? ` / ${bytes(record.backupBytes)}` : ''}</Descriptions.Item>
        <Descriptions.Item label="开始时间">{record.startedAt || '—'}</Descriptions.Item>
        <Descriptions.Item label="结束时间">{record.finishedAt || '—'}</Descriptions.Item>
        <Descriptions.Item label="备注">{record.note || '—'}</Descriptions.Item>
      </Descriptions>
      {record.status === 'SUCCESS' && <Button onClick={() => window.location.reload()}>刷新并使用新版本</Button>}
    </Space>
  </>;
}
