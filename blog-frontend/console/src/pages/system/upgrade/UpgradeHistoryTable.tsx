import React from 'react';
import ProTable from '@ant-design/pro-table';
import { Button, Descriptions, Empty } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import type { UpgradeRecord } from './types';
import { statusLabels } from './utils';

interface Props {
  records: UpgradeRecord[];
  loading: boolean;
  onRefresh: () => void;
  onSelect: (record: UpgradeRecord) => void;
}

export default function UpgradeHistoryTable({ records, loading, onRefresh, onSelect }: Props) {
  return <>
    <ProTable<UpgradeRecord>
        className="upgrade-table"
        rowKey="id"
        headerTitle="升级记录"
        loading={loading}
        dataSource={records}
        search={false}
        options={false}
        scroll={{ x: 1000 }}
        pagination={{ pageSize: 10, showSizeChanger: false }}
        toolBarRender={() => [
          <Button key="refresh" icon={<ReloadOutlined/>} loading={loading}
                  onClick={onRefresh}>刷新记录</Button>,
        ]}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无升级记录"/>,
        }} columns={[
      { title: '升级包', dataIndex: 'packageName' },
      { title: '目标版本', key: 'version', render: (_, item) => `${item.packageVersion} / ${item.packageBuild}` },
      { title: '状态', dataIndex: 'status', render: (_, item) => statusLabels[item.status] || item.status },
      { title: '开始时间', dataIndex: 'startedAt', render: value => value || '—' },
      { title: '结束时间', dataIndex: 'finishedAt', render: value => value || '—' },
      {
        title: '操作',
        key: 'action',
        render: (_, item) => <Button type="link" onClick={() => onSelect(item)}>查看详情</Button>
      },
    ]} expandable={{
      expandedRowRender: item => <Descriptions column={1}>
        <Descriptions.Item label="任务 ID">{item.id}</Descriptions.Item>
        <Descriptions.Item label="原版本">{item.oldVersion || '—'} / {item.oldBuild ?? '—'}</Descriptions.Item>
        <Descriptions.Item label="详情">{item.message || '—'}</Descriptions.Item>
        <Descriptions.Item
            label="参数快照">备份：{item.backupEnabled ? '开启' : '关闭'}；备份超时：{item.backupTimeoutSeconds} 秒；健康检查：{item.healthTimeoutSeconds} 秒</Descriptions.Item>
        <Descriptions.Item label="升级后删除包">{item.deletePackageAfterUpgrade ? '是' : '否'}</Descriptions.Item>
        <Descriptions.Item label="升级包路径">{item.packagePath}</Descriptions.Item>
        <Descriptions.Item label="备份文件">{item.backupPath || '—'}</Descriptions.Item>
        <Descriptions.Item label="发行版本目录">{item.releasePath || '—'}</Descriptions.Item>
        <Descriptions.Item label="备注">{item.note || '—'}</Descriptions.Item>
      </Descriptions>}}/>
    <span className="upgrade-secondary">记录已提交和已完成的升级任务，升级包删除后仍保留快照。</span>
  </>;
}
