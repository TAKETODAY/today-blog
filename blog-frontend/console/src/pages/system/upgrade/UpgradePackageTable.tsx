import React from 'react';
import ProTable from '@ant-design/pro-table';
import { Button, Empty, Space, Tag } from 'antd';
import { ArrowUpOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import type { UpgradePackage } from './types';
import { bytes, statusLabels } from './utils';

interface Props {
  packages: UpgradePackage[];
  busy: boolean;
  onStart: (item: UpgradePackage) => void;
  onEdit: (item: UpgradePackage) => void;
  onRemove: (item: UpgradePackage) => void;
}

export default function UpgradePackageTable({ packages, busy, onStart, onEdit, onRemove }: Props) {
  return <>
    <ProTable<UpgradePackage>
        className="upgrade-table"
        rowKey="id"
        dataSource={packages}
        search={false}
        options={false}
        pagination={false}
        scroll={{ x: 900 }}
        locale={{
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无升级包，请先上传"/>
        }} columns={[
      {
        title: '升级包',
        key: 'name',
        render: (_, item) => <Space direction="vertical" size={0}>
          <span>{item.displayName || item.originalName}</span>
          <span className="upgrade-secondary">{item.originalName}</span>
        </Space>
      },
      { title: '版本 / 构建', key: 'version', render: (_, item) => `${item.version} / ${item.build}` },
      {
        title: '状态',
        dataIndex: 'status',
        render: (_, item) => <Tag color={item.status === 'READY' ? 'success' : 'default'}>{statusLabels[item.status] || item.status}</Tag>
      },
      { title: '大小', dataIndex: 'fileSize', render: value => bytes(Number(value)) },
      { title: '上传时间', dataIndex: 'createdAt' },
      {
        title: '操作',
        key: 'action',
        render: (_, item) => <Space size={0}>
          <Button type="link" disabled={item.status !== 'READY' || busy}
                  onClick={() => onStart(item)} icon={<ArrowUpOutlined/>}>升级</Button>
          <Button type="link" onClick={() => onEdit(item)} icon={<EditOutlined/>}>编辑</Button>
          <Button type="link" danger onClick={() => onRemove(item)} disabled={busy} icon={<DeleteOutlined/>}>删除</Button>
        </Space>
      },
    ]}/>
    <span className="upgrade-secondary">选择待升级的发行包；已使用的包保留历史快照，可继续查看或删除。</span>
  </>;
}
