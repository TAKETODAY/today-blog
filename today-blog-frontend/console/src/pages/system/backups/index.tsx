import React, { useCallback, useEffect, useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { Alert, Button, Card, Checkbox, Descriptions, Drawer, Modal, Space, Table, Tag, Tooltip } from 'antd';
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

const backupTypes = ['DATABASE', 'ATTACHMENTS', 'WEBSITE', 'CONFIGURATION', 'RELEASE', 'OSS'];

interface BackupJob {
  id: string;
  types: string;
  status: string;
  createdAt: string;
  completedAt?: string;
  offsite: boolean;
  archive?: string;
  bytes: number;
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
    CREATING: t('backups.creating'), AVAILABLE: t('backups.available'), FAILED: t('backups.failed'),
    DELETING: t('backups.deleting'), DELETED: t('backups.deleted'), STARTING: t('backups.submitted'),
    SUCCESS: t('backups.succeeded'), RESTORING: t('backups.restoring'), MAINTENANCE_REQUIRED: t('backups.manualActionRequired'),
    OFFSITE_FAILED: t('backups.offsiteFailed'),
  };
  const status = (value: string) => {
    const color = ['AVAILABLE', 'SUCCESS'].includes(value) ? 'success'
      : ['FAILED', 'MAINTENANCE_REQUIRED', 'OFFSITE_FAILED'].includes(value) ? 'error' : 'default';
    return <Tag color={color}>{statusNames[value] || value}</Tag>;
  };
  const actionRef = useRef<ActionType>();
  const [capabilities, setCapabilities] = useState<{ reason?: string; create?: boolean; types?: string[]; offsite?: boolean }>();
  const [jobs, setJobs] = useState<BackupJob[]>([]);
  const [jobsError, setJobsError] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const [offsite, setOffsite] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(false);
  const allowed = backupTypes.filter(type => capabilities?.types?.includes(type));
  const recommended = allowed.filter(type => type !== 'OSS');
  const isFullScope = (types: string[]) => backupTypes.filter(type => type !== 'OSS').every(type => types.includes(type));
  const typeLabel = (type: string) => t(`backups.type.${type}`);
  const [error, setError] = useState(false);
  const [detail, setDetail] = useState<BackupDetail>();
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [detailId, setDetailId] = useState<number>();
  const [downloading, setDownloading] = useState<number>();
  const [downloadError, setDownloadError] = useState(false);
  const reason = error ? t('backups.couldNotCheckBackupReadiness')
    : !capabilities ? t('backups.checkingBackupReadiness')
      : capabilities.reason || t('backups.backupOperationsAreNotAvailable');

  const loadCapabilities = useCallback(async () => {
    setError(false);
    try {
      const response = await http.get('/api/console/backups/capabilities');
      setCapabilities(response.data);
    } catch (failure) {
      setError(true);
    }
  }, []);

  useEffect(() => {
    loadCapabilities();
  }, [loadCapabilities]);

  const loadJobs = useCallback(async () => {
    try {
      const response = await http.get('/api/console/backups/jobs');
      setJobs(response.data);
      setJobsError(false);
    } catch {
      setJobsError(true);
    }
  }, []);

  useEffect(() => {
    if (!capabilities) {
      return undefined;
    }
    loadJobs();
    const timer = window.setInterval(loadJobs, 5000);
    return () => window.clearInterval(timer);
  }, [capabilities, loadJobs]);

  const submit = async () => {
    setSubmitting(true);
    setSubmitError(false);
    try {
      await http.post('/api/console/backups', {types: selected, offsite});
      setCreateOpen(false);
      void loadJobs();
    } catch {
      setSubmitError(true);
    } finally {
      setSubmitting(false);
    }
  };

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

  const download = async (id: number) => {
    setDownloading(id);
    setDownloadError(false);
    const url = `/api/console/backups/${id}/download`;
    try {
      await http.head(url);
      // 交给浏览器下载，不在页面内缓存完整的归档。
      const link = document.createElement('a');
      link.href = url;
      link.download = '';
      document.body.appendChild(link);
      try {
        link.click();
      } finally {
        link.remove();
      }
    } catch {
      setDownloadError(true);
    } finally {
      setDownloading(undefined);
    }
  };

  const unavailable = (label: string, danger = false) => <Tooltip title={reason}>
    <span><Button type="link" danger={danger} disabled>{label}</Button></span>
  </Tooltip>;

  const columns: ProColumns<Backup>[] = [
    {
      title: t('backups.backup'),
      dataIndex: 'name',
      fixed: 'left',
      width: 320,
      render: (_, item) => <Space direction="vertical" size={0}>
        <Button type="link" style={{padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left'}}
                onClick={() => setDetailId(item.id)}>{item.name}</Button>
        <span className="backup-secondary">{item.id}</span>
      </Space>,
    },
    {
      title: t('backups.status'),
      dataIndex: 'status',
      width: 120,
      valueEnum: Object.fromEntries(Object.entries(statusNames).map(([value, text]) => [value, {text}])),
      render: (_, item) => status(item.status),
    },
    {
      title: t('backups.applicationVersion'),
      key: 'version',
      width: 150,
      hideInSearch: true,
      render: (_, item) => `${item.version || '—'} / ${item.build || '—'}`,
    },
    {
      title: t('backups.version'),
      dataIndex: 'version',
      hideInTable: true,
    },
    {
      title: t('backups.size'),
      dataIndex: 'totalBytes',
      width: 110,
      hideInSearch: true,
      render: (_, item) => bytes(item.totalBytes),
    },
    {
      title: t('backups.protection'),
      dataIndex: 'protectedBackup',
      width: 110,
      valueType: 'select',
      valueEnum: {
        true: {text: t('backups.protected')},
        false: {text: t('backups.unprotected')},
      },
      render: (_, item) => item.protectedBackup ? <Tag icon={<LockOutlined/>}>{t('backups.protected')}</Tag> : t('backups.unprotected'),
    },
    {
      title: t('backups.created'),
      dataIndex: 'createdAt',
      width: 210,
      valueType: 'dateTimeRange',
      render: (_, item) => format(item.createdAt),
    },
    {
      title: t('backups.actions'),
      key: 'actions',
      fixed: 'right',
      width: 260,
      valueType: 'option',
      render: (_, item) => <Space size={0}>
        <Button type="link" onClick={() => setDetailId(item.id)}>{t('backups.details')}</Button>
        {item.status === 'AVAILABLE' && <Button type="link" loading={downloading === item.id}
                                                disabled={downloading !== undefined} onClick={() => download(item.id)}>{t('backups.download')}</Button>}
        {unavailable(item.protectedBackup ? t('backups.unprotect') : t('backups.protect'))}
        {unavailable(t('backups.restore'))}
        {unavailable(t('backups.delete'), true)}
      </Space>,
    },
  ];

  return <PageContainer className="backups-page" title={t('backups.systemBackups')}
                        subTitle={t('backups.manageRestorePointsAndTrack')}>
    <Space direction="vertical" size="large" style={{width: '100%'}}>
      <Alert type="warning" showIcon message={t('backups.restoringABackupRollsBack')}
             description={t('backups.aRestoreRevertsTheApplication')}/>
      {(!capabilities?.create || error) && <Alert type={error ? 'error' : 'info'} showIcon message={error ? t('backups.couldNotLoadBackupInformation') : t('backups.backupOperationsAreNotReady')}
             description={error ? t('backups.theListAndEnvironmentStatus') : reason}
             action={error ? <Button size="small" onClick={refresh}>{t('backups.retry')}</Button> : undefined}/>}
      {(capabilities?.create || jobs.length > 0 || jobsError) && <Card title={t('backups.jobs')}>
        {jobsError && <Alert type="warning" showIcon message={t('backups.jobsUnavailable')}/>}
        <Table<BackupJob> rowKey="id" dataSource={jobs} size="small" pagination={{pageSize: 5}}
                          scroll={{x: 800}} columns={[
          {title: t('backups.scope'), render: (_, job) => {
            const types = job.types.split(',');
            return <Space direction="vertical" size={0}>
              <span>{types.map(typeLabel).join(' / ')}</span>
              <Tag>{t(isFullScope(types) ? 'backups.fullLocalScope' : 'backups.partialScope')}</Tag>
            </Space>;
          }},
          {title: t('backups.status'), render: (_, job) => status(job.status)},
          {title: t('backups.created'), render: (_, job) => format(job.createdAt)},
          {title: t('backups.duration'), render: (_, job) => job.completedAt
            ? `${Math.max(0, Math.floor((Date.parse(job.completedAt) - Date.parse(job.createdAt)) / 1000))} s` : '—'},
          {title: t('backups.size'), render: (_, job) => bytes(job.bytes)},
          {title: t('backups.offsite'), render: (_, job) => job.offsite && job.status === 'AVAILABLE' ? t('backups.succeeded') : '—'},
          {title: t('backups.recoveryVerification'), render: (_, job) => job.archive ? t('backups.notVerified') : '—'},
          {title: t('backups.filePath'), dataIndex: 'archive'},
        ]}/>
      </Card>}
      {downloadError && <Alert type="error" showIcon closable onClose={() => setDownloadError(false)}
                               message={t('backups.downloadFailed')}/>}
      <ProTable<Backup>
        rowKey="id"
        actionRef={actionRef}
        headerTitle={t('backups.backups')}
        columns={columns}
        scroll={{x: 1100}}
        search={{labelWidth: 'auto'}}
        pagination={{pageSize: 10, showSizeChanger: false}}
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
          <Button key="refresh" icon={<ReloadOutlined/>} onClick={refresh}>{t('backups.refresh')}</Button>,
          <Tooltip key="create" title={capabilities?.create ? undefined : reason}>
            <span><Button type="primary" icon={<PlusOutlined/>} disabled={!capabilities?.create || error
              || jobs.some(job => ['STARTING', 'CREATING'].includes(job.status))} onClick={() => {
              setSelected(recommended);
              setOffsite(!!capabilities?.offsite);
              setSubmitError(false);
              setCreateOpen(true);
            }}>{t('backups.createBackup')}</Button></span>
          </Tooltip>,
        ]}
      />
      <span className="backup-secondary">{t('backups.showsUpTo500Recent')}</span>
    </Space>
    <Modal title={t('backups.createBackup')} open={createOpen} onCancel={() => setCreateOpen(false)}
           onOk={submit} confirmLoading={submitting} okButtonProps={{disabled: !selected.length || !selected.every(type => allowed.includes(type))}}
           okText={t('backups.createBackup')}>
      <Space direction="vertical" style={{width: '100%'}}>
        <Alert showIcon type="warning" message={t('backups.captureWarning')}/>
        {!!selected.length && <Tag>{t(isFullScope(selected) ? 'backups.fullLocalScope' : 'backups.partialScope')}</Tag>}
        <Space>
          <Button size="small" onClick={() => setSelected(allowed)}>{t('backups.selectAll')}</Button>
          <Button size="small" onClick={() => setSelected(recommended)}>{t('backups.recommended')}</Button>
        </Space>
        <Checkbox.Group value={selected} onChange={values => setSelected(values.map(String))}>
          <Space direction="vertical">
            {backupTypes.map(type => <Checkbox key={type} value={type} disabled={!allowed.includes(type)}>{typeLabel(type)}</Checkbox>)}
          </Space>
        </Checkbox.Group>
        <Checkbox checked={offsite} disabled={!capabilities?.offsite} onChange={event => setOffsite(event.target.checked)}>{t('backups.offsite')}</Checkbox>
        {submitError && <Alert type="error" showIcon message={t('backups.submitFailed')}/>}
      </Space>
    </Modal>
    <Drawer className="backup-detail-drawer" title={t('backups.backupDetails')} open={detailId !== undefined} onClose={() => setDetailId(undefined)} width={640}
            contentWrapperStyle={{maxWidth: '100vw'}}>
      {detailError && <Alert type="error" showIcon message={t('backups.couldNotReadTheBackup')}
                             description={t('backups.theBackupMayHaveBeen')}/>}
      {detailLoading && <Card loading bordered={false}/>}
      {detail && <Space direction="vertical" size="large" style={{width: '100%'}}>
        <Alert type="info" showIcon message={t('backups.verifyBeforeRestoring')}
               description={t('backups.metadataDoesNotReplaceChecksum')}/>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label={t('backups.name')}>{detail.backup.name}</Descriptions.Item>
          <Descriptions.Item label={t('backups.backupID')}><span className="backup-id">{detail.backup.id}</span></Descriptions.Item>
          <Descriptions.Item label={t('backups.status')}>{status(detail.backup.status)}</Descriptions.Item>
          <Descriptions.Item label={t('backups.applicationVersion')}>{detail.backup.version || '—'} / {detail.backup.build || '—'}</Descriptions.Item>
          <Descriptions.Item label={t('backups.created')}>{format(detail.backup.createdAt)}</Descriptions.Item>
          <Descriptions.Item label={t('backups.protectionStatus')}>{detail.backup.protectedBackup ? t('backups.protectedFromDeletion') : t('backups.unprotected')}</Descriptions.Item>
          <Descriptions.Item label={t('backups.size')}>{bytes(detail.backup.totalBytes)}</Descriptions.Item>
          <Descriptions.Item label={t('backups.filePath')}><span className="backup-id">{detail.location || '—'}</span></Descriptions.Item>
          <Descriptions.Item label={t('backups.scope')}>{detail.scope || t('backups.notRecordedDoNotAssume')}</Descriptions.Item>
          <Descriptions.Item label={t('backups.manifestChecksum')}><span
            className="backup-id">{detail.backup.manifestSha256 || '—'}</span></Descriptions.Item>
          <Descriptions.Item label={t('backups.restoreTest')}>{format(detail.restoreVerifiedAt) || t('backups.noVerifiedRestoreOnRecord')}</Descriptions.Item>
          <Descriptions.Item label={t('backups.sourceTask')}><span
            className="backup-id">{detail.backup.sourceOperationId || t('backups.manualOrNoLinkedTask')}</span></Descriptions.Item>
        </Descriptions>
        <Button onClick={() => history.push('/system/upgrade')}>{t('backups.goToSystemUpgrade')}</Button>
      </Space>}
    </Drawer>
  </PageContainer>;
}
