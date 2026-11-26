import React from 'react';
import ProTable from '@ant-design/pro-table';
import { Button, Descriptions, Empty } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { format } from '@/utils/site-format';
import type { UpgradeRecord } from './types';
import { statusLabel } from './utils';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  records: UpgradeRecord[];
  loading: boolean;
  onRefresh: () => void;
  onSelect: (record: UpgradeRecord) => void;
}

export default function UpgradeHistoryTable({ records, loading, onRefresh, onSelect }: Props) {
  const t = useSiteTranslation();
  return <>
    <ProTable<UpgradeRecord>
        className="upgrade-table"
        rowKey="id"
        headerTitle={t('upgrade.upgradeHistory')}
        loading={loading}
        dataSource={records}
        search={false}
        options={false}
        scroll={{ x: 1000 }}
        pagination={{ pageSize: 10, showSizeChanger: false }}
        toolBarRender={() => [
          <Button key="refresh" icon={<ReloadOutlined/>} loading={loading}
                  onClick={onRefresh}>{t('upgrade.refreshHistory')}</Button>,
        ]}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t('upgrade.noUpgradeHistory')}/>,
        }} columns={[
      { title: t('upgrade.package'), dataIndex: 'packageName' },
      { title: t('upgrade.targetVersion'), key: 'version', render: (_, item) => `${item.packageVersion} / ${item.packageBuild}` },
      { title: t('upgrade.status'), dataIndex: 'status', render: (_, item) => statusLabel(item.status) },
      { title: t('upgrade.started'), dataIndex: 'startedAt', render: (_, item) => format(item.startedAt) || '—' },
      { title: t('upgrade.finished'), dataIndex: 'finishedAt', render: (_, item) => format(item.finishedAt) || '—' },
      {
        title: t('upgrade.actions'),
        key: 'action',
        render: (_, item) => <Button type="link" onClick={() => onSelect(item)}>{t('upgrade.viewDetails')}</Button>
      },
    ]} expandable={{
      expandedRowRender: item => <Descriptions column={1}>
        <Descriptions.Item label={t('upgrade.taskID')}>{item.id}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.previousVersion')}>{item.oldVersion || '—'} / {item.oldBuild ?? '—'}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.details')}>{item.message || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.savedParameters')}>
          {t('upgrade.historyParameters', {
            backup: item.backupEnabled ? 'enabled' : 'disabled',
            backupTimeout: item.backupTimeoutSeconds,
            healthTimeout: item.healthTimeoutSeconds,
          })}
        </Descriptions.Item>
        <Descriptions.Item label={t('upgrade.deletePackageAfterUpgrade')}>{item.deletePackageAfterUpgrade ? t('upgrade.yes') : t('upgrade.no')}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.packagePath')}>{item.packagePath}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.backupFile')}>{item.backupPath || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.releaseDirectory')}>{item.releasePath || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('upgrade.notes')}>{item.note || '—'}</Descriptions.Item>
      </Descriptions>}}/>
    <span className="upgrade-secondary">{t('upgrade.showsSubmittedAndCompletedUpgrades')}</span>
  </>;
}
