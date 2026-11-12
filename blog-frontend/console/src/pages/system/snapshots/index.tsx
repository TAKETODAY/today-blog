import React, { useCallback, useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Alert, Button, Card, Descriptions, Drawer, Empty, Input, Space, Table, Tabs, Tag, Tooltip } from 'antd';
import { LockOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { http } from '@/utils/http';
import './index.less';

interface Snapshot {
  id: string;
  name: string;
  status: string;
  version: string;
  build: string;
  createdAt: string;
  protected: string;
  totalBytes: string;
  sourceOperationId: string;
  [key: string]: string;
}

interface Operation {
  id: string;
  type: string;
  status: string;
  createdAt: string;
  finishedAt: string;
  snapshotId: string;
  protectionSnapshotId: string;
  operatorId: string;
}

const statusNames: Record<string, string> = {
  CREATING: '创建中', AVAILABLE: '可用', FAILED: '失败', DELETING: '删除中', DELETED: '已删除',
  STARTING: '已提交', SUCCESS: '成功', RESTORING: '恢复中', MAINTENANCE_REQUIRED: '需人工处理',
};
const typeNames: Record<string, string> = {
  SNAPSHOT_CREATE: '创建快照', SNAPSHOT_RESTORE: '恢复快照', SNAPSHOT_DELETE: '删除快照',
  SNAPSHOT_PROTECT: '修改保护', UPGRADE: '系统更新',
};

function status(value: string) {
  const color = ['AVAILABLE', 'SUCCESS'].includes(value) ? 'success'
    : ['FAILED', 'MAINTENANCE_REQUIRED'].includes(value) ? 'error' : 'default';
  return <Tag color={color}>{statusNames[value] || value}</Tag>;
}

function bytes(value: string) {
  const size = Number(value);
  if (!Number.isFinite(size) || size < 0) {
    return '—';
  }
  if (size >= 1024 ** 3) {
    return `${(size / 1024 ** 3).toFixed(2)} GiB`;
  }
  return `${(size / 1024 ** 2).toFixed(1)} MiB`;
}

export default function AppSnapshots() {
  const [snapshots, setSnapshots] = useState<Snapshot[]>([]);
  const [operations, setOperations] = useState<Operation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [reason, setReason] = useState('正在检查快照执行环境');
  const [query, setQuery] = useState('');
  const [detail, setDetail] = useState<Snapshot>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [detailId, setDetailId] = useState('');

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const [records, actions, capabilities] = await Promise.all([
        http.get('/api/console/snapshots'), http.get('/api/console/snapshots/operations'),
        http.get('/api/console/snapshots/capabilities'),
      ]);
      setSnapshots(records.data);
      setOperations(actions.data);
      setReason(capabilities.data.reason || '快照执行能力尚不可用');
    } catch (failure) {
      setError(true);
      setReason('无法确认快照执行环境，请刷新重试');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    if (!detailId) {
      return undefined;
    }
    let cancelled = false;
    setDetail(undefined);
    setDetailLoading(true);
    setDetailError(false);
    http.get(`/api/console/snapshots/${detailId}`).then(response => {
      if (!cancelled) {
        setDetail(response.data);
      }
    }).catch(() => {
      if (!cancelled) {
        setDetailError(true);
      }
    }).finally(() => {
      if (!cancelled) {
        setDetailLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [detailId]);

  const filtered = snapshots.filter(item => `${item.name} ${item.version} ${item.build} ${item.id}`.toLowerCase().includes(query.toLowerCase()));
  const unavailable = (label: string, danger = false) => <Tooltip title={reason}>
    <span><Button type="link" danger={danger} disabled>{label}</Button></span>
  </Tooltip>;

  return <PageContainer className="app-snapshots-page" title="应用快照" subTitle="管理应用恢复点，追踪程序与数据的匹配状态">
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message="恢复快照会回退业务数据"
        description="恢复会将程序、数据库、附件及配置回到同一恢复点，此后新增或修改的数据可能丢失。恢复前必须先创建当前状态的保护快照。" />
      <Alert type={error ? 'error' : 'info'} showIcon message={error ? '快照数据加载失败' : '快照执行能力未就绪'}
        description={error ? '未能取得最新列表和环境状态，以下内容可能不是最新数据。请重试，勿重复提交操作。' : reason}
        action={error ? <Button size="small" onClick={refresh}>重试</Button> : undefined} />
      <Card>
        <Tabs items={[
          {
            key: 'snapshots', label: '快照列表', children: <>
              <div className="snapshot-toolbar">
                <Input.Search aria-label="搜索快照" placeholder="搜索名称、版本或快照 ID" allowClear value={query}
                  onChange={event => setQuery(event.target.value)} style={{ maxWidth: 360 }} />
                <Space>
                  <Button icon={<ReloadOutlined />} onClick={refresh} loading={loading}>刷新</Button>
                  <Tooltip title={reason}><span><Button type="primary" icon={<PlusOutlined />} disabled>创建快照</Button></span></Tooltip>
                </Space>
              </div>
              <Table<Snapshot> rowKey="id" loading={loading} dataSource={filtered} scroll={{ x: 1050 }}
                pagination={{ pageSize: 10, showSizeChanger: false }}
                locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={error ? '快照列表暂不可用' : query ? '没有匹配的快照' : '暂无应用快照'} /> }} columns={[
                  { title: '快照', key: 'name', width: 230, render: (_, item) => <Space direction="vertical" size={0}>
                    <Button type="link" style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }} onClick={() => setDetailId(item.id)}>{item.name}</Button>
                    <span className="snapshot-secondary">{item.version || '—'} / {item.build || '—'}</span>
                  </Space> },
                  { title: '状态', dataIndex: 'status', render: status, width: 110 },
                  { title: '创建时间', dataIndex: 'createdAt', width: 210 },
                  { title: '大小', dataIndex: 'totalBytes', render: bytes, width: 100 },
                  { title: '保护', dataIndex: 'protected', width: 100, render: value => value === 'true' ? <Tag icon={<LockOutlined />}>已保护</Tag> : '未保护' },
                  { title: '操作', key: 'actions', width: 270, render: (_, item) => <Space size={0}>
                    <Button type="link" onClick={() => setDetailId(item.id)}>详情</Button>
                    {unavailable(item.protected === 'true' ? '取消保护' : '保护')}
                    {unavailable('恢复')}
                    {unavailable('删除', true)}
                  </Space> },
                ]} />
              <span className="snapshot-secondary">最多展示最近 500 份快照。列表记录不代表备份已通过完整性校验或恢复演练。</span>
            </>,
          },
          {
            key: 'operations', label: '操作记录', children: <>
              <div className="snapshot-toolbar"><span>创建、恢复、保护和删除的操作记录，以及关联快照的系统更新</span><Button onClick={refresh} loading={loading}>刷新记录</Button></div>
              <Table<Operation> rowKey="id" loading={loading} dataSource={operations} scroll={{ x: 1100 }}
                pagination={{ pageSize: 10, showSizeChanger: false }} columns={[
                  { title: '操作', dataIndex: 'type', render: value => typeNames[value] || value },
                  { title: '状态', dataIndex: 'status', render: status },
                  { title: '操作人 ID', dataIndex: 'operatorId' },
                  { title: '开始时间', dataIndex: 'createdAt' },
                  { title: '结束时间', dataIndex: 'finishedAt', render: value => value || '—' },
                  { title: '目标快照', dataIndex: 'snapshotId', render: value => value ? <Button type="link" onClick={() => setDetailId(value)}>查看快照</Button> : '—' },
                  { title: '保护快照', dataIndex: 'protectionSnapshotId', render: value => value ? <Button type="link" onClick={() => setDetailId(value)}>查看保护快照</Button> : '—' },
                ]} expandable={{ expandedRowRender: item => <Descriptions column={1}>
                  <Descriptions.Item label="操作 ID">{item.id}</Descriptions.Item>
                  <Descriptions.Item label="目标快照 ID">{item.snapshotId || '—'}</Descriptions.Item>
                  <Descriptions.Item label="保护快照 ID">{item.protectionSnapshotId || '—'}</Descriptions.Item>
                </Descriptions> }} />
            </>,
          },
        ]} />
      </Card>
    </Space>
    <Drawer className="snapshot-detail-drawer" title="快照详情" open={Boolean(detailId)} onClose={() => setDetailId('')} width={640}
      contentWrapperStyle={{ maxWidth: '100vw' }}>
      {detailError && <Alert type="error" showIcon message="无法读取快照详情" description="快照可能已移除或连接中断，请关闭后重试。" />}
      {detailLoading && <Card loading bordered={false} />}
      {detail && <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Alert type="info" showIcon message="恢复前仍需重新校验" description="元信息不能代替文件摘要检查。创建成功也不代表已经通过恢复演练。" />
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="名称">{detail.name}</Descriptions.Item>
          <Descriptions.Item label="快照 ID"><span className="snapshot-id">{detail.id}</span></Descriptions.Item>
          <Descriptions.Item label="状态">{status(detail.status)}</Descriptions.Item>
          <Descriptions.Item label="程序版本">{detail.version || '—'} / {detail.build || '—'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{detail.createdAt}</Descriptions.Item>
          <Descriptions.Item label="操作人 ID">{detail.operatorId || '—'}</Descriptions.Item>
          <Descriptions.Item label="保护状态">{detail.protected === 'true' ? '已保护，禁止删除' : '未保护'}</Descriptions.Item>
          <Descriptions.Item label="大小">{bytes(detail.totalBytes)}</Descriptions.Item>
          <Descriptions.Item label="覆盖范围">{detail.scope || '尚未记录，不可据此判断为完整快照'}</Descriptions.Item>
          <Descriptions.Item label="清单摘要"><span className="snapshot-id">{detail.manifestSha256 || '—'}</span></Descriptions.Item>
          <Descriptions.Item label="恢复演练">{detail.restoreVerifiedAt || '尚无恢复验证记录'}</Descriptions.Item>
          <Descriptions.Item label="来源任务"><span className="snapshot-id">{detail.sourceOperationId || '手动创建或未关联任务'}</span></Descriptions.Item>
        </Descriptions>
        <Button onClick={() => history.push('/system/upgrade')}>前往系统更新</Button>
      </Space>}
    </Drawer>
  </PageContainer>;
}
