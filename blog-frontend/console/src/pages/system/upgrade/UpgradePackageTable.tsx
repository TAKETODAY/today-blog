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
          emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t('upgrade.uploadAReleasePackageTo')}/>
        }} columns={[
      {
        title: t('upgrade.packages'),
        key: 'name',
        render: (_, item) => <Space direction="vertical" size={0}>
          <span>{item.displayName || item.originalName}</span>
          <span className="upgrade-secondary">{item.originalName}</span>
        </Space>
      },
      { title: t('upgrade.versionBuild'), key: 'version', render: (_, item) => `${item.version} / ${item.build}` },
      {
        title: t('upgrade.status'),
        dataIndex: 'status',
        render: (_, item) => <Tag color={item.status === 'READY' ? 'success' : 'default'}>{statusLabel(item.status)}</Tag>
      },
      { title: t('upgrade.size'), dataIndex: 'fileSize', render: value => bytes(Number(value)) },
      { title: t('upgrade.uploaded'), dataIndex: 'createdAt', render: (_, item) => format(item.createdAt) },
      {
        title: t('upgrade.actions'),
        key: 'action',
        render: (_, item) => <Space size={0}>
          <Button type="link" disabled={item.status !== 'READY' || busy}
                  onClick={() => onStart(item)} icon={<ArrowUpOutlined/>}>{t('upgrade.upgrade')}</Button>
          <Button type="link" onClick={() => onEdit(item)} icon={<EditOutlined/>}>{t('upgrade.edit')}</Button>
          <Button type="link" danger onClick={() => onRemove(item)} disabled={busy} icon={<DeleteOutlined/>}>{t('upgrade.delete')}</Button>
        </Space>
      },
    ]}/>
    <span className="upgrade-secondary">{t('upgrade.chooseAReleasePackageTo')}</span>
  </>;
}
