import React, { useCallback, useEffect, useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { Alert, Button, Card, Descriptions, Drawer, Space, Tag, Tooltip } from 'antd';
import { LockOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { http } from '@/utils/http';
import './index.less';

interface Backup {
  id: number;
  name: string;
  status: string;
  version: string | null;
  build: string | null;
  createdAt: string;
  protectedBackup: boolean;
  totalBytes: number;
  operatorId?: number | null;
  manifestSha256: string | null;
  sourceOperationId: number | null;
}

interface BackupDetail {
  backup: Backup;
  scope: string | null;
  restoreVerifiedAt: string | null;
  location: string | null;
}

const statusNames: Record<string, string> = {
  CREATING: '创建中', AVAILABLE: '可用', FAILED: '失败', DELETING: '删除中', DELETED: '已删除',
  STARTING: '已提交', SUCCESS: '成功', RESTORING: '恢复中', MAINTENANCE_REQUIRED: '需人工处理',
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
  const actionRef = useRef<ActionType>();
  const [reason, setReason] = useState('正在检查备份执行环境');
  const [error, setError] = useState(false);
  const [detail, setDetail] = useState<BackupDetail>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [detailId, setDetailId] = useState<number>();

  const loadCapabilities = useCallback(async () => {
    setError(false);
    try {
      const response = await http.get('/api/console/backups/capabilities');
      setReason(response.data.reason || '备份执行能力尚不可用');
    }
    catch (failure) {
      setError(true);
      setReason('无法确认备份执行环境，请刷新重试');
    }
  }, []);

  useEffect(() => {
    loadCapabilities();
  }, [loadCapabilities]);

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

  const refresh = () => {
    actionRef.current?.reload();
    loadCapabilities();
  };

  const unavailable = (label: string, danger = false) => <Tooltip title={reason}>
    <span><Button type="link" danger={danger} disabled>{label}</Button></span>
  </Tooltip>;

  const columns: ProColumns<Backup>[] = [
    {
      title: '备份',
      dataIndex: 'name',
      fixed: 'left',
      width: 260,
      render: (_, item) => <Space direction="vertical" size={0}>
        <Button type="link" style={{ padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }}
                onClick={() => setDetailId(item.id)}>{item.name}</Button>
        <span className="backup-secondary">{item.id}</span>
      </Space>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      valueEnum: Object.fromEntries(Object.entries(statusNames).map(([value, text]) => [value, { text }])),
      render: (_, item) => status(item.status),
    },
    {
      title: '程序版本',
      key: 'version',
      width: 150,
      hideInSearch: true,
      render: (_, item) => `${item.version || '—'} / ${item.build || '—'}`,
    },
    {
      title: '版本',
      dataIndex: 'version',
      hideInTable: true,
    },
    {
      title: '大小',
      dataIndex: 'totalBytes',
      width: 110,
      hideInSearch: true,
      render: (_, item) => bytes(item.totalBytes),
    },
    {
      title: '保护',
      dataIndex: 'protectedBackup',
      width: 110,
      valueType: 'select',
      valueEnum: {
        true: { text: '已保护' },
        false: { text: '未保护' },
      },
      render: (_, item) => item.protectedBackup ? <Tag icon={<LockOutlined/>}>已保护</Tag> : '未保护',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 210,
      valueType: 'dateTimeRange',
      render: (_, item) => item.createdAt,
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 260,
      valueType: 'option',
      render: (_, item) => <Space size={0}>
        <Button type="link" onClick={() => setDetailId(item.id)}>详情</Button>
        {unavailable(item.protectedBackup ? '取消保护' : '保护')}
        {unavailable('恢复')}
        {unavailable('删除', true)}
      </Space>,
    },
  ];

  return <PageContainer className="backups-page" title="系统备份" subTitle="管理恢复点，追踪程序与数据的匹配状态">
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message="恢复备份会回退业务数据"
             description="恢复会将程序、数据库、附件及配置回到同一恢复点，此后新增或修改的数据可能丢失。恢复前必须先自动备份当前状态并保护该备份。"/>
      <Alert type={error ? 'error' : 'info'} showIcon message={error ? '备份数据加载失败' : '备份执行能力未就绪'}
             description={error ? '未能取得最新列表和环境状态，以下内容可能不是最新数据。请重试，勿重复提交操作。' : reason}
             action={error ? <Button size="small" onClick={refresh}>重试</Button> : undefined}/>
      <ProTable<Backup>
          rowKey="id"
          actionRef={actionRef}
          headerTitle="备份列表"
          columns={columns}
          scroll={{ x: 1100 }}
          search={{ labelWidth: 'auto' }}
          pagination={{ pageSize: 10, showSizeChanger: false }}
          request={async params => {
            const response = await http.get('/api/console/backups');
            let records = response.data as Backup[];
            if (params.name) {
              const query = String(params.name).toLowerCase();
              records = records.filter(item => `${item.name} ${item.id} ${item.version || ''}`.toLowerCase().includes(query));
            }
            if (params.status) {
              records = records.filter(item => item.status === params.status);
            }
            if (params.version) {
              records = records.filter(item => (item.version || '').includes(String(params.version)));
            }
            if (Array.isArray(params.createdAt) && params.createdAt.length === 2) {
              const [from, to] = params.createdAt;
              const fromTime = from ? Date.parse(String(from).replace(' ', 'T')) : NaN;
              const toTime = to ? Date.parse(String(to).replace(' ', 'T')) : NaN;
              records = records.filter(item => {
                const created = Date.parse(item.createdAt);
                return (Number.isNaN(fromTime) || created >= fromTime)
                    && (Number.isNaN(toTime) || created <= toTime);
              });
            }
            if (params.protectedBackup !== undefined && params.protectedBackup !== '') {
              const wanted = params.protectedBackup === true || params.protectedBackup === 'true';
              records = records.filter(item => item.protectedBackup === wanted);
            }
            const current = params.current || 1;
            const pageSize = params.pageSize || 10;
            return {
              data: records.slice((current - 1) * pageSize, current * pageSize),
              total: records.length,
              success: true,
            };
          }}
          toolBarRender={() => [
            <Button key="refresh" icon={<ReloadOutlined/>} onClick={refresh}>刷新</Button>,
            <Tooltip key="create" title={reason}>
              <span><Button type="primary" icon={<PlusOutlined/>} disabled>创建备份</Button></span>
            </Tooltip>,
          ]}
      />
      <span className="backup-secondary">最多展示最近 500 份备份。列表记录不代表备份已通过完整性校验或恢复演练。</span>
    </Space>
    <Drawer className="backup-detail-drawer" title="备份详情" open={detailId !== undefined} onClose={() => setDetailId(undefined)} width={640}
            contentWrapperStyle={{ maxWidth: '100vw' }}>
      {detailError && <Alert type="error" showIcon message="无法读取备份详情" description="备份可能已移除或连接中断，请关闭后重试。"/>}
      {detailLoading && <Card loading bordered={false}/>}
      {detail && <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Alert type="info" showIcon message="恢复前仍需重新校验"
               description="元信息不能代替文件摘要检查。创建成功也不代表已经通过恢复演练。"/>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="名称">{detail.backup.name}</Descriptions.Item>
          <Descriptions.Item label="备份 ID"><span className="backup-id">{detail.backup.id}</span></Descriptions.Item>
          <Descriptions.Item label="状态">{status(detail.backup.status)}</Descriptions.Item>
          <Descriptions.Item label="程序版本">{detail.backup.version || '—'} / {detail.backup.build || '—'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{detail.backup.createdAt}</Descriptions.Item>
          <Descriptions.Item label="保护状态">{detail.backup.protectedBackup ? '已保护，禁止删除' : '未保护'}</Descriptions.Item>
          <Descriptions.Item label="大小">{bytes(detail.backup.totalBytes)}</Descriptions.Item>
          <Descriptions.Item label="文件路径"><span className="backup-id">{detail.location || '—'}</span></Descriptions.Item>
          <Descriptions.Item label="覆盖范围">{detail.scope || '尚未记录，不可据此判断为完整备份'}</Descriptions.Item>
          <Descriptions.Item label="清单摘要"><span
            className="backup-id">{detail.backup.manifestSha256 || '—'}</span></Descriptions.Item>
          <Descriptions.Item label="恢复演练">{detail.restoreVerifiedAt || '尚无恢复验证记录'}</Descriptions.Item>
          <Descriptions.Item label="来源任务"><span
            className="backup-id">{detail.backup.sourceOperationId || '手动创建或未关联任务'}</span></Descriptions.Item>
        </Descriptions>
        <Button onClick={() => history.push('/system/upgrade')}>前往系统升级</Button>
      </Space>}
    </Drawer>
  </PageContainer>;
}
