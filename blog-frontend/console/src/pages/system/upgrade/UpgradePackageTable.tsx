import React from 'react';
import ProTable from '@ant-design/pro-table';
import { Button, Empty, Space, Tag } from 'antd';
import { ArrowUpOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { format } from '@/utils/site-format';
import type { UpgradePackage } from './types';
import { bytes, statusLabel } from './utils';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  packages: UpgradePackage[];
  busy: boolean;
  onStart: (item: UpgradePackage) => void;
  onEdit: (item: UpgradePackage) => void;
  onRemove: (item: UpgradePackage) => void;
}

export default function UpgradePackageTable({ packages, busy, onStart, onEdit, onRemove }: Props) {
  const t = useSiteTranslation();
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
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t('暂无升级包，请先上传', 'Upload a release package to get started')}/>
        }} columns={[
      {
        title: t('升级包', 'Packages'),
        key: 'name',
        render: (_, item) => <Space direction="vertical" size={0}>
          <span>{item.displayName || item.originalName}</span>
          <span className="upgrade-secondary">{item.originalName}</span>
        </Space>
      },
      { title: t('版本 / 构建', 'Version / build'), key: 'version', render: (_, item) => `${item.version} / ${item.build}` },
      {
        title: t('状态', 'Status'),
        dataIndex: 'status',
        render: (_, item) => <Tag color={item.status === 'READY' ? 'success' : 'default'}>{statusLabel(item.status)}</Tag>
      },
      { title: t('大小', 'Size'), dataIndex: 'fileSize', render: value => bytes(Number(value)) },
      { title: t('上传时间', 'Uploaded'), dataIndex: 'createdAt', render: (_, item) => format(item.createdAt) },
      {
        title: t('操作', 'Actions'),
        key: 'action',
        render: (_, item) => <Space size={0}>
          <Button type="link" disabled={item.status !== 'READY' || busy}
                  onClick={() => onStart(item)} icon={<ArrowUpOutlined/>}>{t('升级', 'Upgrade')}</Button>
          <Button type="link" onClick={() => onEdit(item)} icon={<EditOutlined/>}>{t('编辑', 'Edit')}</Button>
          <Button type="link" danger onClick={() => onRemove(item)} disabled={busy} icon={<DeleteOutlined/>}>{t('删除', 'Delete')}</Button>
        </Space>
      },
    ]}/>
    <span className="upgrade-secondary">{t('选择待升级的发行包；已使用的包保留历史快照，可继续查看或删除。',
      'Choose a release package to upgrade. You can still view or delete used packages; upgrade history retains a snapshot.')}</span>
  </>;
}
