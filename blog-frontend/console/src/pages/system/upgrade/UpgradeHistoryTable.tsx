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
        headerTitle={t('升级记录', 'Upgrade history')}
        loading={loading}
        dataSource={records}
        search={false}
        options={false}
        scroll={{ x: 1000 }}
        pagination={{ pageSize: 10, showSizeChanger: false }}
        toolBarRender={() => [
          <Button key="refresh" icon={<ReloadOutlined/>} loading={loading}
                  onClick={onRefresh}>{t('刷新记录', 'Refresh history')}</Button>,
        ]}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t('暂无升级记录', 'No upgrade history')}/>,
        }} columns={[
      { title: t('升级包', 'Package'), dataIndex: 'packageName' },
      { title: t('目标版本', 'Target version'), key: 'version', render: (_, item) => `${item.packageVersion} / ${item.packageBuild}` },
      { title: t('状态', 'Status'), dataIndex: 'status', render: (_, item) => statusLabel(item.status) },
      { title: t('开始时间', 'Started'), dataIndex: 'startedAt', render: (_, item) => format(item.startedAt) || '—' },
      { title: t('结束时间', 'Finished'), dataIndex: 'finishedAt', render: (_, item) => format(item.finishedAt) || '—' },
      {
        title: t('操作', 'Actions'),
        key: 'action',
        render: (_, item) => <Button type="link" onClick={() => onSelect(item)}>{t('查看详情', 'View details')}</Button>
      },
    ]} expandable={{
      expandedRowRender: item => <Descriptions column={1}>
        <Descriptions.Item label={t('任务 ID', 'Task ID')}>{item.id}</Descriptions.Item>
        <Descriptions.Item label={t('原版本', 'Previous version')}>{item.oldVersion || '—'} / {item.oldBuild ?? '—'}</Descriptions.Item>
        <Descriptions.Item label={t('详情', 'Details')}>{item.message || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('参数快照', 'Saved parameters')}>
          {t(`备份：${item.backupEnabled ? '开启' : '关闭'}；备份超时：${item.backupTimeoutSeconds} 秒；健康检查：${item.healthTimeoutSeconds} 秒`,
            `Backup: ${item.backupEnabled ? 'enabled' : 'disabled'}; backup timeout: ${item.backupTimeoutSeconds}s; health check: ${item.healthTimeoutSeconds}s`)}
        </Descriptions.Item>
        <Descriptions.Item label={t('升级后删除包', 'Delete package after upgrade')}>{item.deletePackageAfterUpgrade ? t('是', 'Yes') : t('否', 'No')}</Descriptions.Item>
        <Descriptions.Item label={t('升级包路径', 'Package path')}>{item.packagePath}</Descriptions.Item>
        <Descriptions.Item label={t('备份文件', 'Backup file')}>{item.backupPath || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('发行版本目录', 'Release directory')}>{item.releasePath || '—'}</Descriptions.Item>
        <Descriptions.Item label={t('备注', 'Notes')}>{item.note || '—'}</Descriptions.Item>
      </Descriptions>}}/>
    <span className="upgrade-secondary">{t('记录已提交和已完成的升级任务，升级包删除后仍保留快照。',
      'Shows submitted and completed upgrades. Snapshots remain after a package is deleted.')}</span>
  </>;
}
