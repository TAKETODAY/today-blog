import React, { useCallback, useEffect, useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { Alert, Button, Card, Descriptions, Drawer, Space, Tag, Tooltip } from 'antd';
import { LockOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { http } from '@/utils/http';
import { format } from '@/utils/site-format';
import { useSiteTranslation } from '@/utils/use-site-translation';
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
  const t = useSiteTranslation();
  const statusNames: Record<string, string> = {
    CREATING: t('创建中', 'Creating'), AVAILABLE: t('可用', 'Available'), FAILED: t('失败', 'Failed'),
    DELETING: t('删除中', 'Deleting'), DELETED: t('已删除', 'Deleted'), STARTING: t('已提交', 'Submitted'),
    SUCCESS: t('成功', 'Succeeded'), RESTORING: t('恢复中', 'Restoring'), MAINTENANCE_REQUIRED: t('需人工处理', 'Manual action required'),
  };
  const status = (value: string) => {
    const color = ['AVAILABLE', 'SUCCESS'].includes(value) ? 'success'
        : ['FAILED', 'MAINTENANCE_REQUIRED'].includes(value) ? 'error' : 'default';
    return <Tag color={color}>{statusNames[value] || value}</Tag>;
  };
  const actionRef = useRef<ActionType>();
  const [capabilities, setCapabilities] = useState<{ reason?: string }>();
  const [error, setError] = useState(false);
  const [detail, setDetail] = useState<BackupDetail>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [detailId, setDetailId] = useState<number>();
  const reason = error ? t('无法确认备份执行环境，请刷新重试', 'Could not check backup readiness. Refresh and try again.')
      : !capabilities ? t('正在检查备份执行环境', 'Checking backup readiness')
      : capabilities.reason || t('备份执行能力尚不可用', 'Backup operations are not available');

  const loadCapabilities = useCallback(async () => {
    setError(false);
    try {
      const response = await http.get('/api/console/backups/capabilities');
      setCapabilities(response.data);
    }
    catch (failure) {
      setError(true);
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
      title: t('备份', 'Backup'),
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
      title: t('状态', 'Status'),
      dataIndex: 'status',
      width: 120,
      valueEnum: Object.fromEntries(Object.entries(statusNames).map(([value, text]) => [value, { text }])),
      render: (_, item) => status(item.status),
    },
    {
      title: t('程序版本', 'Application version'),
      key: 'version',
      width: 150,
      hideInSearch: true,
      render: (_, item) => `${item.version || '—'} / ${item.build || '—'}`,
    },
    {
      title: t('版本', 'Version'),
      dataIndex: 'version',
      hideInTable: true,
    },
    {
      title: t('大小', 'Size'),
      dataIndex: 'totalBytes',
      width: 110,
      hideInSearch: true,
      render: (_, item) => bytes(item.totalBytes),
    },
    {
      title: t('保护', 'Protection'),
      dataIndex: 'protectedBackup',
      width: 110,
      valueType: 'select',
      valueEnum: {
        true: { text: t('已保护', 'Protected') },
        false: { text: t('未保护', 'Unprotected') },
      },
      render: (_, item) => item.protectedBackup ? <Tag icon={<LockOutlined/>}>{t('已保护', 'Protected')}</Tag> : t('未保护', 'Unprotected'),
    },
    {
      title: t('创建时间', 'Created'),
      dataIndex: 'createdAt',
      width: 210,
      valueType: 'dateTimeRange',
      render: (_, item) => format(item.createdAt),
    },
    {
      title: t('操作', 'Actions'),
      key: 'actions',
      fixed: 'right',
      width: 260,
      valueType: 'option',
      render: (_, item) => <Space size={0}>
        <Button type="link" onClick={() => setDetailId(item.id)}>{t('详情', 'Details')}</Button>
        {unavailable(item.protectedBackup ? t('取消保护', 'Unprotect') : t('保护', 'Protect'))}
        {unavailable(t('恢复', 'Restore'))}
        {unavailable(t('删除', 'Delete'), true)}
      </Space>,
    },
  ];

  return <PageContainer className="backups-page" title={t('系统备份', 'System backups')}
                        subTitle={t('管理恢复点，追踪程序与数据的匹配状态', 'Manage restore points and track matching application and data versions')}>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message={t('恢复备份会回退业务数据', 'Restoring a backup rolls back data')}
             description={t('恢复会将程序、数据库、附件及配置回到同一恢复点，此后新增或修改的数据可能丢失。恢复前必须先自动备份当前状态并保护该备份。',
               'A restore reverts the application, database, attachments and settings to the same point. Later changes may be lost. Back up and protect the current state automatically before restoring.')}/>
      <Alert type={error ? 'error' : 'info'} showIcon message={error ? t('备份数据加载失败', 'Could not load backup information') : t('备份执行能力未就绪', 'Backup operations are not ready')}
             description={error ? t('未能取得最新列表和环境状态，以下内容可能不是最新数据。请重试，勿重复提交操作。',
               'The list and environment status may be outdated. Try again without resubmitting operations.') : reason}
             action={error ? <Button size="small" onClick={refresh}>{t('重试', 'Retry')}</Button> : undefined}/>
      <ProTable<Backup>
          rowKey="id"
          actionRef={actionRef}
          headerTitle={t('备份列表', 'Backups')}
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
            <Button key="refresh" icon={<ReloadOutlined/>} onClick={refresh}>{t('刷新', 'Refresh')}</Button>,
            <Tooltip key="create" title={reason}>
              <span><Button type="primary" icon={<PlusOutlined/>} disabled>{t('创建备份', 'Create backup')}</Button></span>
            </Tooltip>,
          ]}
      />
      <span className="backup-secondary">{t('最多展示最近 500 份备份。列表记录不代表备份已通过完整性校验或恢复演练。',
        'Shows up to 500 recent backups. Listing does not imply integrity verification or a successful restore test.')}</span>
    </Space>
    <Drawer className="backup-detail-drawer" title={t('备份详情', 'Backup details')} open={detailId !== undefined} onClose={() => setDetailId(undefined)} width={640}
            contentWrapperStyle={{ maxWidth: '100vw' }}>
      {detailError && <Alert type="error" showIcon message={t('无法读取备份详情', 'Could not read the backup')}
                            description={t('备份可能已移除或连接中断，请关闭后重试。', 'The backup may have been removed, or the connection was lost. Close and try again.')}/>}
      {detailLoading && <Card loading bordered={false}/>}
      {detail && <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Alert type="info" showIcon message={t('恢复前仍需重新校验', 'Verify before restoring')}
               description={t('元信息不能代替文件摘要检查。创建成功也不代表已经通过恢复演练。', 'Metadata does not replace checksum verification. A successful backup does not imply a successful restore test.')}/>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label={t('名称', 'Name')}>{detail.backup.name}</Descriptions.Item>
          <Descriptions.Item label={t('备份 ID', 'Backup ID')}><span className="backup-id">{detail.backup.id}</span></Descriptions.Item>
          <Descriptions.Item label={t('状态', 'Status')}>{status(detail.backup.status)}</Descriptions.Item>
          <Descriptions.Item label={t('程序版本', 'Application version')}>{detail.backup.version || '—'} / {detail.backup.build || '—'}</Descriptions.Item>
          <Descriptions.Item label={t('创建时间', 'Created')}>{format(detail.backup.createdAt)}</Descriptions.Item>
          <Descriptions.Item label={t('保护状态', 'Protection')}>{detail.backup.protectedBackup ? t('已保护，禁止删除', 'Protected from deletion') : t('未保护', 'Unprotected')}</Descriptions.Item>
          <Descriptions.Item label={t('大小', 'Size')}>{bytes(detail.backup.totalBytes)}</Descriptions.Item>
          <Descriptions.Item label={t('文件路径', 'File path')}><span className="backup-id">{detail.location || '—'}</span></Descriptions.Item>
          <Descriptions.Item label={t('覆盖范围', 'Scope')}>{detail.scope || t('尚未记录，不可据此判断为完整备份', 'Not recorded; do not assume this is a full backup')}</Descriptions.Item>
          <Descriptions.Item label={t('清单摘要', 'Manifest checksum')}><span
            className="backup-id">{detail.backup.manifestSha256 || '—'}</span></Descriptions.Item>
          <Descriptions.Item label={t('恢复演练', 'Restore test')}>{format(detail.restoreVerifiedAt) || t('尚无恢复验证记录', 'No verified restore on record')}</Descriptions.Item>
          <Descriptions.Item label={t('来源任务', 'Source task')}><span
            className="backup-id">{detail.backup.sourceOperationId || t('手动创建或未关联任务', 'Manual or no linked task')}</span></Descriptions.Item>
        </Descriptions>
        <Button onClick={() => history.push('/system/upgrade')}>{t('前往系统升级', 'Go to system upgrade')}</Button>
      </Space>}
    </Drawer>
  </PageContainer>;
}
