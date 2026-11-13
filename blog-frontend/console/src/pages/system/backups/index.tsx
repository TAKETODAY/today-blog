import React, { useCallback, useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Alert, Button, Card, Descriptions, Drawer, Empty, Input, Space, Table, Tabs, Tag, Tooltip } from 'antd';
import { LockOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { http } from '@/utils/http';
import './index.less';

interface Backup {
  id: string;
  name: string;
  status: string;
  version: string | null;
  build: string | null;
  createdAt: string;
  protectedSnapshot: boolean;
  totalBytes: number;
  operatorId: number | null;
  manifestSha256: string | null;
  sourceOperationId: string | null;
}

interface BackupDetail {
  snapshot: Backup;
  scope: string | null;
  restoreVerifiedAt: string | null;
}

interface Operation {
  id: string;
  type: string;
  status: string;
  createdAt: string;
  finishedAt: string | null;
  snapshotId: string | null;
  protectionSnapshotId: string | null;
  operatorId: number | null;
}

const statusNames: Record<string, string> = {
  CREATING: '创建中', AVAILABLE: '可用', FAILED: '失败', DELETING: '删除中', DELETED: '已删除',
  STARTING: '已提交', SUCCESS: '成功', RESTORING: '恢复中', MAINTENANCE_REQUIRED: '需人工处理',
};
const typeNames: Record<string, string> = {
  SNAPSHOT_CREATE: '创建备份', SNAPSHOT_RESTORE: '恢复备份', SNAPSHOT_DELETE: '删除备份',
  SNAPSHOT_PROTECT: '修改保护', UPGRADE: '系统更新',
};

function status(value: string) {
  const color = ['AVAILABLE', 'SUCCESS'].includes(value) ? 'success'
      : ['FAILED', 'MAINTENANCE_REQUIRED'].includes(value) ? 'error' : 'default';
  return <Tag color={color}>{statusNames[value] || value}</Tag>;
}

function bytes(value: number) {
  const size = Number(value);
  if (!Number.isFinite(size) || size < 0) {
    return '—';
  }
  if (size >= 1024 ** 3) {
    return `${(size / 1024 ** 3).toFixed(2)} GiB`;
  }
  return `${(size / 1024 ** 2).toFixed(1)} MiB`;
}

export default function Backups() {
  const [backups, setBackups] = useState<Backup[]>([]);
  const [operations, setOperations] = useState<Operation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [reason, setReason] = useState('正在检查备份执行环境');
  const [query, setQuery] = useState('');
  const [detail, setDetail] = useState<BackupDetail>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [detailId, setDetailId] = useState('');

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const [records, actions, capabilities] = await Promise.all([
        http.get('/api/console/backups'),
        http.get('/api/console/backups/operations'),
        http.get('/api/console/backups/capabilities'),
      ]);
      setBackups(records.data);
      setOperations(actions.data);
      setReason(capabilities.data.reason || '备份执行能力尚不可用');
    }
    catch (failure) {
      setError(true);
      setReason('无法确认备份执行环境，请刷新重试');
    }
    finally {
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
    http.get(`/api/console/backups/${detailId}`).then(response => {
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

  const filtered = backups.filter(item => `${item.name} ${item.version} ${item.build} ${item.id}`.toLowerCase().includes(query.toLowerCase()));
  const unavailable = (label: string, danger = false) => <Tooltip title={reason}>
    <span><Button type="link" danger={danger} disabled>{label}</Button></span>
  </Tooltip>;

  return <PageContainer className="backups-page" title="备份管理" subTitle="管理恢复点，追踪程序与数据的匹配状态">
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message="恢复备份会回退业务数据"
             description="恢复会将程序、数据库、附件及配置回到同一恢复点，此后新增或修改的数据可能丢失。恢复前必须先自动备份当前状态并保护该备份。"/>
      <Alert type={error ? 'error' : 'info'} showIcon message={error ? '备份数据加载失败' : '备份执行能力未就绪'}
             description={error ? '未能取得最新列表和环境状态，以下内容可能不是最新数据。请重试，勿重复提交操作。' : reason}
             action={error ? <Button size="small" onClick={refresh}>重试</Button> : undefined}/>
      <Card>
        <Tabs items={[
          {
            key: 'backups', label: '备份列表', children: <>
              <div className="snapshot-toolbar">
                <Input.Search aria-label="搜索备份" placeholder="搜索名称、版本或备份 ID" allowClear value={query}
                              onChange={event => setQuery(event.target.value)} style={{ maxWidth: 360 }}/>
                <Space>
                  <Button icon={<ReloadOutlined/>} onClick={refresh} loading={loading}>刷新</Button>
                  <Tooltip title={reason}><span><Button type="primary" icon={<PlusOutlined/>} disabled>创建备份</Button></span></Tooltip>
                </Space>
              </div>
              <Table<Backup> rowKey="id" loading={loading} dataSource={filtered} scroll={{ x: 1050 }}
                               pagination={{ pageSize: 10, showSizeChanger: false }}
                               locale={{
                                 emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}
                                                   description={error ? '备份列表暂不可用' : query ? '没有匹配的备份' : '暂无备份'}/>
                               }} columns={[
                {
                  title: '备份', key: 'name', width: 230, render: (_, item) => <Space direction="vertical" size={0}>
                    <Button type="link" style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }}
                            onClick={() => setDetailId(item.id)}>{item.name}</Button>
                    <span className="snapshot-secondary">{item.version || '—'} / {item.build || '—'}</span>
                  </Space>
                },
                { title: '状态', dataIndex: 'status', render: status, width: 110 },
                { title: '创建时间', dataIndex: 'createdAt', width: 210 },
                { title: '大小', dataIndex: 'totalBytes', render: bytes, width: 100 },
                {
                  title: '保护',
                  dataIndex: 'protectedSnapshot',
                  width: 100,
                  render: value => value ? <Tag icon={<LockOutlined/>}>已保护</Tag> : '未保护'
                },
                {
                  title: '操作', key: 'actions', width: 270, render: (_, item) => <Space size={0}>
                    <Button type="link" onClick={() => setDetailId(item.id)}>详情</Button>
                    {unavailable(item.protectedSnapshot ? '取消保护' : '保护')}
                    {unavailable('恢复')}
                    {unavailable('删除', true)}
                  </Space>
                },
              ]}/>
              <span className="snapshot-secondary">最多展示最近 500 份备份。列表记录不代表备份已通过完整性校验或恢复演练。</span>
            </>,
          },
          {
            key: 'operations', label: '操作记录', children: <>
              <div className="snapshot-toolbar"><span>创建、恢复、保护和删除的操作记录，以及关联备份的系统更新</span><Button onClick={refresh}
                                                                                                                          loading={loading}>刷新记录</Button>
              </div>
              <Table<Operation> rowKey="id" loading={loading} dataSource={operations} scroll={{ x: 1100 }}
                                pagination={{ pageSize: 10, showSizeChanger: false }} columns={[
                { title: '操作', dataIndex: 'type', render: value => typeNames[value] || value },
                { title: '状态', dataIndex: 'status', render: status },
                { title: '操作人 ID', dataIndex: 'operatorId' },
                { title: '开始时间', dataIndex: 'createdAt' },
                { title: '结束时间', dataIndex: 'finishedAt', render: value => value || '—' },
                {
                  title: '目标备份',
                  dataIndex: 'snapshotId',
                  render: value => value ? <Button type="link" onClick={() => setDetailId(value)}>查看备份</Button> : '—'
                },
                {
                  title: '保护备份',
                  dataIndex: 'protectionSnapshotId',
                  render: value => value ? <Button type="link" onClick={() => setDetailId(value)}>查看保护备份</Button> : '—'
                },
              ]} expandable={{
                expandedRowRender: item => <Descriptions column={1}>
                  <Descriptions.Item label="操作 ID">{item.id}</Descriptions.Item>
                  <Descriptions.Item label="目标备份 ID">{item.snapshotId || '—'}</Descriptions.Item>
                  <Descriptions.Item label="保护备份 ID">{item.protectionSnapshotId || '—'}</Descriptions.Item>
                </Descriptions>
              }}/>
            </>,
          },
        ]}/>
      </Card>
    </Space>
    <Drawer className="snapshot-detail-drawer" title="备份详情" open={Boolean(detailId)} onClose={() => setDetailId('')} width={640}
            contentWrapperStyle={{ maxWidth: '100vw' }}>
      {detailError && <Alert type="error" showIcon message="无法读取备份详情" description="备份可能已移除或连接中断，请关闭后重试。"/>}
      {detailLoading && <Card loading bordered={false}/>}
      {detail && <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Alert type="info" showIcon message="恢复前仍需重新校验"
               description="元信息不能代替文件摘要检查。创建成功也不代表已经通过恢复演练。"/>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="名称">{detail.snapshot.name}</Descriptions.Item>
          <Descriptions.Item label="备份 ID"><span className="snapshot-id">{detail.snapshot.id}</span></Descriptions.Item>
          <Descriptions.Item label="状态">{status(detail.snapshot.status)}</Descriptions.Item>
          <Descriptions.Item label="程序版本">{detail.snapshot.version || '—'} / {detail.snapshot.build || '—'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{detail.snapshot.createdAt}</Descriptions.Item>
          <Descriptions.Item label="操作人 ID">{detail.snapshot.operatorId ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="保护状态">{detail.snapshot.protectedSnapshot ? '已保护，禁止删除' : '未保护'}</Descriptions.Item>
          <Descriptions.Item label="大小">{bytes(detail.snapshot.totalBytes)}</Descriptions.Item>
          <Descriptions.Item label="覆盖范围">{detail.scope || '尚未记录，不可据此判断为完整备份'}</Descriptions.Item>
          <Descriptions.Item label="清单摘要"><span
            className="snapshot-id">{detail.snapshot.manifestSha256 || '—'}</span></Descriptions.Item>
          <Descriptions.Item label="恢复演练">{detail.restoreVerifiedAt || '尚无恢复验证记录'}</Descriptions.Item>
          <Descriptions.Item label="来源任务"><span
            className="snapshot-id">{detail.snapshot.sourceOperationId || '手动创建或未关联任务'}</span></Descriptions.Item>
        </Descriptions>
        <Button onClick={() => history.push('/system/upgrade')}>前往系统更新</Button>
      </Space>}
    </Drawer>
  </PageContainer>;
}
