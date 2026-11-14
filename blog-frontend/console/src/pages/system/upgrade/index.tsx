import React, { useCallback, useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable from '@ant-design/pro-table';
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Divider,
  Empty,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Steps,
  Switch,
  Tabs,
  Tag,
  Upload
} from 'antd';
import {
  MinusCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { http } from '@/utils/http';
import './index.less';

interface Options {
  backupEnabled: boolean;
  backupTimeoutSeconds: number;
  healthTimeoutSeconds: number;
  note: string;
}

interface Environment {
  updaterReady: boolean;
  backupReady: boolean;
  installFreeBytes: number;
  backupFreeBytes: number;
}

interface ReleaseMapping {
  name: string;
  mode: 'SYMLINK';
  source: string;
  target: string;
}

interface MappingSettings {
  mappings: ReleaseMapping[];
  roots: string[];
  ready: boolean;
  message: string;
}

interface UpgradePackage {
  id: number;
  originalName: string;
  displayName: string;
  note?: string;
  status: 'READY' | 'CONSUMED';
  version: string;
  build: number;
  fileSize: number;
  sha256: string;
  entryCount: number;
  location: string;
  createdAt: string;
  consumedAt?: string;
}

interface UpgradeRecord {
  id: number;
  packageId: number;
  packageName: string;
  packageVersion: string;
  packageBuild: number;
  packageSha256: string;
  packagePath: string;
  oldVersion?: string;
  oldBuild?: number;
  status: string;
  backupEnabled: boolean;
  backupTimeoutSeconds: number;
  healthTimeoutSeconds: number;
  note?: string;
  deletePackageAfterUpgrade: boolean;
  releasePath?: string;
  backupPath?: string;
  backupBytes?: number;
  backupSha256?: string;
  backupCompletedAt?: string;
  message?: string;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
}

interface MaintenanceStatus {
  enabled: boolean;
  message?: string;
  retryAfterSeconds?: number;
  source?: 'upgrade' | 'manual';
}

const terminal = ['SUCCESS', 'FAILED', 'ROLLED_BACK'];
const taskKey = 'today-blog-upgrade-task';
const phases = ['VALIDATING', 'INSTALLING', 'BACKING_UP', 'RESTARTING', 'HEALTH_CHECKING', 'SUCCESS'];
const statusLabels: Record<string, string> = {
  STARTING: '已提交', VALIDATING: '校验升级包', INSTALLING: '准备版本',
  BACKING_UP: '数据库备份', RESTARTING: '切换并重启', HEALTH_CHECKING: '健康检查',
  SUCCESS: '升级成功', FAILED: '升级失败', ROLLING_BACK: '正在恢复旧程序', ROLLED_BACK: '升级失败，已恢复旧版本',
  READY: '待升级', CONSUMED: '已使用',
};

function parseOptions(data?: Partial<Options>): Options {
  return {
    backupEnabled: data?.backupEnabled ?? true,
    backupTimeoutSeconds: data?.backupTimeoutSeconds ?? 600,
    healthTimeoutSeconds: data?.healthTimeoutSeconds ?? 90,
    note: data?.note || '',
  };
}

function requestOptions(options: Options): Options {
  return { ...options, note: options.note || '' };
}

function bytes(value?: number) {
  const size = Number(value);
  if (!Number.isFinite(size) || size < 0) {
    return '—';
  }
  if (size >= 1024 ** 3) {
    return `${(size / 1024 ** 3).toFixed(2)} GiB`;
  }
  return `${(size / 1024 ** 2).toFixed(1)} MiB`;
}

function OptionFields() {
  return <>
    <Form.Item name="backupEnabled" label="升级前备份数据库" valuePropName="checked"
               extra="备份失败将终止升级；关闭后必须自行保证有可恢复的备份。">
      <Switch checkedChildren="开启" unCheckedChildren="关闭"/>
    </Form.Item>
    <Form.Item name="backupTimeoutSeconds" label="备份超时（秒）" rules={[{ required: true }]}>
      <InputNumber min={30} max={3600} precision={0}/>
    </Form.Item>
    <Form.Item name="healthTimeoutSeconds" label="健康检查超时（秒）" rules={[{ required: true }]}>
      <InputNumber min={10} max={600} precision={0}/>
    </Form.Item>
  </>;
}

export default function SystemUpgrade() {
  const [version, setVersion] = useState<{ version: string; build?: string | null }>({ version: '加载中' });
  const [record, setRecord] = useState<UpgradeRecord>();
  const [id, setId] = useState(() => localStorage.getItem(taskKey) || '');
  const [records, setRecords] = useState<UpgradeRecord[]>([]);
  const [packages, setPackages] = useState<UpgradePackage[]>([]);
  const [startPackage, setStartPackage] = useState<UpgradePackage>();
  const [environment, setEnvironment] = useState<Environment>();
  const [uploading, setUploading] = useState(false);
  const [starting, setStarting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [tab, setTab] = useState('run');
  const [disconnected, setDisconnected] = useState(false);
  const [needsLogin, setNeedsLogin] = useState(false);
  const [maintenance, setMaintenance] = useState<MaintenanceStatus>();
  const [maintenanceSaving, setMaintenanceSaving] = useState(false);
  const [maintenanceVisible, setMaintenanceVisible] = useState(false);
  const [mappingSettings, setMappingSettings] = useState<MappingSettings>();
  const [mappingSaving, setMappingSaving] = useState(false);
  const [configVisible, setConfigVisible] = useState(false);
  const [configTab, setConfigTab] = useState('settings');
  const [uploadVisible, setUploadVisible] = useState(false);
  const [uploadFile, setUploadFile] = useState<File>();
  const [editPackage, setEditPackage] = useState<UpgradePackage>();
  const [editName, setEditName] = useState('');
  const [editNote, setEditNote] = useState('');
  const [packageSaving, setPackageSaving] = useState(false);
  const [deletePackageAfter, setDeletePackageAfter] = useState(false);
  const [runForm] = Form.useForm<Options>();
  const [settingsForm] = Form.useForm<Options>();
  const [mappingForm] = Form.useForm<{ mappings: ReleaseMapping[] }>();
  const [maintenanceForm] = Form.useForm<{ message: string; retryAfter: number }>();
  const backupEnabled = Form.useWatch('backupEnabled', runForm);

  const selectRecord = useCallback((next: UpgradeRecord) => {
    setRecord(next);
    setTab('run');
    setId(String(next.id));
    localStorage.setItem(taskKey, String(next.id));
  }, []);

  const reloadPackages = useCallback(async () => {
    const response = await http.get('/api/console/maintenance/upgrade/packages');
    const list = response.data as UpgradePackage[];
    setPackages(list);
    return list;
  }, []);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      const [current, history, env, maintenanceStatus] = await Promise.all([
        http.get('/api/console/maintenance/upgrade/current'),
        http.get('/api/console/maintenance/upgrade/records'),
        http.get('/api/console/maintenance/upgrade/environment'),
        http.get('/api/console/maintenance'),
      ]);
      setVersion(current.data);
      setRecords(history.data);
      setEnvironment(env.data);
      setMaintenance(maintenanceStatus.data);
      return history.data as UpgradeRecord[];
    }
    finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [settings, mappings, history] = await Promise.all([
          http.get('/api/console/maintenance/upgrade/settings'),
          http.get('/api/console/maintenance/upgrade/mappings'),
          refresh(),
          reloadPackages(),
        ]);
        if (cancelled) {
          return;
        }
        const options = parseOptions(settings.data);
        runForm.setFieldsValue(options);
        settingsForm.setFieldsValue(options);
        setMappingSettings(mappings.data);
        mappingForm.setFieldsValue({ mappings: mappings.data.mappings || [] });
        const active = history.find(item => !terminal.includes(item.status));
        if (active) {
          selectRecord(active);
        }
        setLoaded(true);
      }
      catch (error) {
        message.error('无法加载升级环境，请刷新页面重试');
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [refresh, reloadPackages, runForm, settingsForm, mappingForm, selectRecord]);

  useEffect(() => {
    if (!id) {
      return undefined;
    }
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      let done = false;
      try {
        const response = await http.get(`/api/console/maintenance/upgrade/records/${id}`);
        if (cancelled) {
          return;
        }
        setRecord(response.data);
        setDisconnected(false);
        setNeedsLogin(false);
        done = terminal.includes(response.data.status);
        if (done) {
          await refresh();
          await reloadPackages();
        }
      }
      catch (error) {
        if (!cancelled) {
          setDisconnected(true);
          setNeedsLogin(error?.response?.status === 401);
        }
      }
      if (!cancelled && !done) {
        timer = setTimeout(poll, 4000);
      }
    }

    poll();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [id, starting, refresh, reloadPackages]);

  const activeTask = records.some(item => !terminal.includes(item.status));
  const busy = uploading || starting || packageSaving || activeTask;
  const environmentReady = loaded && environment?.updaterReady && environment.installFreeBytes >= 3 * 1024 ** 3
      && (!backupEnabled || (environment.backupReady && environment.backupFreeBytes >= 256 * 1024 ** 2));
  const lastPhase = record ? phases.reduce((last, phase, index) => record.status === phase ? index : last, 0) : 0;

  async function uploadPackage() {
    if (!uploadFile) {
      message.warning('请选择升级包');
      return;
    }
    setUploading(true);
    try {
      const data = new FormData();
      data.append('file', uploadFile);
      await http.post('/api/console/maintenance/upgrade/packages', data, { timeout: 300000 });
      setUploadVisible(false);
      setUploadFile(undefined);
      await reloadPackages();
      message.success('升级包完整性校验通过，请确认来源可信');
    }
    finally {
      setUploading(false);
    }
  }

  function openEditPackage(item: UpgradePackage) {
    setEditPackage(item);
    setEditName(item.displayName || item.originalName);
    setEditNote(item.note || '');
  }

  async function savePackage() {
    if (!editPackage) {
      return;
    }
    if (!editName.trim()) {
      message.warning('请输入升级包名称');
      return;
    }
    setPackageSaving(true);
    try {
      await http.put(`/api/console/maintenance/upgrade/packages/${editPackage.id}`, {
        displayName: editName.trim(), note: editNote,
      });
      setEditPackage(undefined);
      await reloadPackages();
      message.success('升级包已更新');
    }
    finally {
      setPackageSaving(false);
    }
  }

  function removePackage(item: UpgradePackage) {
    Modal.confirm({
      title: `删除升级包「${item.displayName || item.originalName}」？`,
      content: '将同时删除数据库记录和本地 ZIP 文件。已被升级记录使用的包会保留历史快照。',
      okText: '删除',
      okButtonProps: { danger: true },
      async onOk() {
        await http.delete(`/api/console/maintenance/upgrade/packages/${item.id}`);
        await reloadPackages();
        message.success('升级包已删除');
      },
    });
  }

  function openStart(item: UpgradePackage) {
    setStartPackage(item);
    setDeletePackageAfter(false);
  }

  async function start() {
    if (!startPackage) {
      return;
    }
    const options = await runForm.validateFields();
    setStarting(true);
    try {
      const response = await http.post(`/api/console/maintenance/upgrade/packages/${startPackage.id}/start`, {
        options: requestOptions(options), deletePackage: deletePackageAfter,
      });
      setStartPackage(undefined);
      selectRecord(response.data);
      await refresh();
      await reloadPackages();
    }
    finally {
      setStarting(false);
    }
  }

  async function saveSettings() {
    const options = await settingsForm.validateFields();
    setSaving(true);
    try {
      const response = await http.put('/api/console/maintenance/upgrade/settings', requestOptions(options));
      settingsForm.setFieldsValue(parseOptions(response.data));
      message.success('默认设置已保存；本次升级参数保持不变');
    }
    finally {
      setSaving(false);
    }
  }

  async function saveMappings() {
    const values = await mappingForm.validateFields();
    setMappingSaving(true);
    try {
      const response = await http.put('/api/console/maintenance/upgrade/mappings', {
        mappings: (values.mappings || []).map(mapping => ({
          ...mapping,
          mode: mapping.mode || 'SYMLINK',
          target: (mapping.target || '').trim(),
        })),
      });
      setMappingSettings(response.data);
      mappingForm.setFieldsValue({ mappings: response.data.mappings || [] });
      message.success('文件映射已保存；已提交的升级任务不受影响');
    }
    finally {
      setMappingSaving(false);
    }
  }

  async function reloadMappings() {
    const response = await http.get('/api/console/maintenance/upgrade/mappings');
    setMappingSettings(response.data);
    mappingForm.setFieldsValue({ mappings: response.data.mappings || [] });
  }

  function addMapping() {
    const root = mappingSettings?.roots?.[0] || '/var/www/today-blog';
    const mappings = mappingForm.getFieldValue('mappings') || [];
    const used = new Set(mappings.map((mapping: ReleaseMapping) => mapping.name));
    const name = used.has('console') ? (used.has('desktop') ? `mapping-${mappings.length + 1}` : 'desktop') : 'console';
    mappingForm.setFieldsValue({
      mappings: [...mappings, {
        name,
        mode: 'SYMLINK',
        source: name === 'desktop' ? 'desktop' : 'console',
        target: `${root}/${name}`,
      }],
    });
  }

  function openConfig(tabKey?: string) {
    if (tabKey) {
      setConfigTab(tabKey);
    }
    setConfigVisible(true);
  }

  function openMaintenanceModal() {
    maintenanceForm.setFieldsValue({ message: '系统维护中，请稍后重试', retryAfter: 120 });
    setMaintenanceVisible(true);
  }

  async function enableMaintenance() {
    const values = await maintenanceForm.validateFields();
    setMaintenanceSaving(true);
    try {
      const response = await http.put('/api/console/maintenance', {
        enabled: true, message: values.message, retryAfter: values.retryAfter,
      });
      setMaintenance(response.data);
      setMaintenanceVisible(false);
      message.success('维护状态已开启，业务请求将返回 503');
    }
    finally {
      setMaintenanceSaving(false);
    }
  }

  function disableMaintenance() {
    Modal.confirm({
      title: '关闭维护状态？',
      content: '关闭后业务请求将立即恢复。若升级仍在进行，请勿关闭。',
      okText: '关闭维护',
      async onOk() {
        setMaintenanceSaving(true);
        try {
          const response = await http.put('/api/console/maintenance', { enabled: false });
          setMaintenance(response.data);
          message.success('维护状态已关闭');
        }
        finally {
          setMaintenanceSaving(false);
        }
      },
    });
  }

  const maintenanceSource = maintenance?.source === 'manual' ? '手动开启，升级后不会自动关闭' : '升级流程自动开启，完成后自动关闭';

  return <PageContainer className="system-update-page" title="系统升级" subTitle="发布新版本，保留可追溯的备份与升级记录"
                        extra={<Space wrap size={8} align="center">
                          <span className="upgrade-secondary">当前版本：{version.version} / {version.build || '—'}</span>
                          <Tag color={maintenance?.enabled ? 'warning' : 'success'}>
                            {maintenance?.enabled ? (maintenance.source === 'manual' ? '手动维护中' : '升级维护中') : '正常服务'}
                          </Tag>
                          <Tag color={environment?.updaterReady ? 'success' : 'warning'}>
                            升级器{environment?.updaterReady ? '已就绪' : '未就绪'}
                          </Tag>
                          {maintenance?.enabled
                              ? <Button danger loading={maintenanceSaving} disabled={busy || !loaded}
                                        onClick={disableMaintenance}>关闭维护</Button>
                              : <Button loading={maintenanceSaving} disabled={busy || !loaded}
                                        onClick={openMaintenanceModal}>开启维护</Button>}
                          <Button icon={<ReloadOutlined/>} loading={refreshing}
                                  onClick={() => refresh().catch(() => message.error('刷新环境失败'))}>刷新状态</Button>
                          <Badge dot={Boolean(mappingSettings && !mappingSettings.ready)} offset={[-4, 4]}>
                            <Button icon={<SettingOutlined/>} onClick={() => openConfig()}>升级配置</Button>
                          </Badge>
                        </Space>}>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message="程序回滚不等于数据库恢复"
             description="仅发布数据库迁移向后兼容的版本。在线备份针对 InnoDB，不包含附件，也不包含备份时间点之后的新数据；升级期间不得并发修改表结构。"/>
      {maintenance?.enabled && <Alert type="warning" showIcon message="维护状态已开启"
                                      description={`${maintenance.message || '系统维护中，请稍后重试'}；建议重试间隔 ${maintenance.retryAfterSeconds || 120} 秒。${maintenanceSource}。`}/>}
      {disconnected && <Alert type="info" showIcon message={needsLogin ? '登录已失效，请重新登录后查看任务' : '连接暂时中断，正在自动重试'}
                              description="任务在服务器独立运行，请勿重复提交。长时间未恢复时请通过 SSH 检查 systemd 日志。"/>}
      <Card>
        <Tabs activeKey={tab} onChange={setTab}
              tabBarExtraContent={tab === 'run'
                  ? <Button icon={<UploadOutlined/>} onClick={() => setUploadVisible(true)}
                            disabled={!loaded}>上传升级包</Button>
                  : undefined}
              items={[
          {
            key: 'run',
            label: '执行升级',
            children: <>
              <Space direction="vertical" size="large" style={{ width: '100%' }}>
                {loaded && mappingSettings && !mappingSettings.ready && <Alert type="warning" showIcon
                    message="文件映射配置未就绪"
                    description={mappingSettings.message || '升级时将无法发布前端文件，请检查服务器目标根目录配置。'}
                    action={<Button size="small" onClick={() => openConfig('mappings')}>去配置</Button>}/>}
                <Descriptions column={{ xs: 1, sm: 2, lg: 3 }} size="small" bordered>
                  <Descriptions.Item label="数据库备份"><Tag
                      color={environment?.backupReady ? 'success' : 'warning'}>{environment?.backupReady ? '已配置，执行时验证连接' : '未就绪'}</Tag></Descriptions.Item>
                  <Descriptions.Item label="安装盘可用空间">{environment ? bytes(environment.installFreeBytes) : '—'}（至少 3
                    GiB）</Descriptions.Item>
                  <Descriptions.Item label="备份盘可用空间">{environment ? bytes(environment.backupFreeBytes) : '—'}（至少 256
                    MiB，另需足够导出空间）</Descriptions.Item>
                </Descriptions>
                <div>
                  <ProTable<UpgradePackage>
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
                    { title: '状态', dataIndex: 'status', render: (_, item) => <Tag color={item.status === 'READY' ? 'success' : 'default'}>{statusLabels[item.status] || item.status}</Tag> },
                    { title: '大小', dataIndex: 'fileSize', render: value => bytes(Number(value)) },
                    { title: '上传时间', dataIndex: 'createdAt' },
                    {
                      title: '操作',
                      key: 'action',
                      render: (_, item) => <Space size={0}>
                        <Button type="link" disabled={item.status !== 'READY' || busy}
                                onClick={() => openStart(item)}>升级</Button>
                        <Button type="link" onClick={() => openEditPackage(item)}>编辑</Button>
                        <Button type="link" danger onClick={() => removePackage(item)} disabled={busy}>删除</Button>
                      </Space>
                    },
                  ]}/>
                  <span className="upgrade-secondary">选择待升级的发行包；已使用的包保留历史快照，可继续查看或删除。</span>
                </div>
                {record && <>
                  <Divider orientation="left" plain>升级任务</Divider>
                  <Space direction="vertical" size="large" style={{ width: '100%' }}>
                    <Alert showIcon
                           type={record.status === 'SUCCESS' ? 'success' : ['FAILED', 'ROLLED_BACK'].includes(record.status) ? 'error' : 'info'}
                           message={statusLabels[record.status] || record.status} description={record.message}/>
                    <Steps size="small" responsive current={phases.includes(record.status) ? phases.indexOf(record.status) : lastPhase}
                           status={['FAILED', 'ROLLED_BACK'].includes(record.status) ? 'error' : undefined}
                           items={phases.map(phase => ({ title: statusLabels[phase] }))}/>
                    <Descriptions column={{ xs: 1, sm: 2 }}>
                      <Descriptions.Item label="升级包">{record.packageName}</Descriptions.Item>
                      <Descriptions.Item label="目标版本">{record.packageVersion} / {record.packageBuild}</Descriptions.Item>
                      <Descriptions.Item label="任务 ID">{record.id}</Descriptions.Item>
                      <Descriptions.Item
                        label="数据库备份">{record.backupPath || '尚未执行'}{record.backupBytes ? ` / ${bytes(record.backupBytes)}` : ''}</Descriptions.Item>
                      <Descriptions.Item label="开始时间">{record.startedAt || '—'}</Descriptions.Item>
                      <Descriptions.Item label="结束时间">{record.finishedAt || '—'}</Descriptions.Item>
                      <Descriptions.Item label="备注">{record.note || '—'}</Descriptions.Item>
                    </Descriptions>
                    {record.status === 'SUCCESS' && <Button onClick={() => window.location.reload()}>刷新并使用新版本</Button>}
                  </Space>
                </>}
              </Space>
            </>,
          },
          {
            key: 'history',
            label: '升级记录',
            children: <>
              <ProTable<UpgradeRecord>
                  rowKey="id"
                  headerTitle="升级记录"
                  loading={refreshing}
                  dataSource={records}
                  search={false}
                  options={false}
                  scroll={{ x: 1000 }}
                  pagination={{ pageSize: 10, showSizeChanger: false }}
                  toolBarRender={() => [
                    <Button key="refresh" icon={<ReloadOutlined/>} loading={refreshing}
                            onClick={() => refresh().catch(() => message.error('刷新记录失败'))}>刷新记录</Button>,
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
                  render: (_, item) => <Button type="link" onClick={() => selectRecord(item)}>查看详情</Button>
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
            </>,
          },
        ]}/>
      </Card>
    </Space>
    <Modal className="upgrade-config-modal" title="升级配置" open={configVisible} forceRender footer={null}
           width={760} style={{ maxWidth: 'calc(100vw - 32px)', top: 40 }}
           bodyStyle={{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto' }}
           onCancel={() => setConfigVisible(false)}>
      <Tabs activeKey={configTab} onChange={setConfigTab} items={[
        {
          key: 'settings',
          label: '升级设置',
          forceRender: true,
          children: <>
            <div className="upgrade-toolbar">
              <span className="upgrade-secondary">默认设置仅用于以后载入页面时初始化参数，不影响已提交的任务；命令及数据库凭据由运维配置。</span>
              <Button type="primary" onClick={saveSettings} loading={saving}
                      disabled={!loaded || !environment?.updaterReady}>保存默认设置</Button>
            </div>
            <Form form={settingsForm} layout="vertical" disabled={!loaded || saving}>
              <OptionFields/>
            </Form>
          </>,
        },
        {
          key: 'mappings',
          label: '文件映射',
          forceRender: true,
          children: <>
            <div className="upgrade-toolbar">
              <span className="upgrade-secondary">映射在切换版本后发布为指向 current/&lt;source&gt; 的符号链接，回滚时自动指回旧版本。</span>
              <Space>
                <Button icon={<ReloadOutlined/>}
                        onClick={() => reloadMappings().catch(() => message.error('重新载入映射失败'))}>重新载入</Button>
                <Button type="primary" loading={mappingSaving}
                        disabled={!loaded || !mappingSettings?.ready || !environment?.updaterReady}
                        onClick={saveMappings}>保存映射</Button>
              </Space>
            </div>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Alert type={mappingSettings?.ready ? 'info' : 'warning'} showIcon
                     message={mappingSettings?.ready ? '映射目标受服务器白名单限制' : '映射目标根目录配置未就绪'}
                     description={mappingSettings?.ready
                         ? '目标必须位于服务器白名单目录内；已存在的非空目录不会被覆盖，发布失败会终止本次升级。'
                         : mappingSettings?.message || '正在加载映射配置'}/>
              <div>
                <span style={{ marginRight: 8 }}>允许的目标根目录：</span>
                {mappingSettings?.roots?.length
                    ? mappingSettings.roots.map(root => <Tag key={root}>{root}</Tag>)
                    : <Tag color="warning">未配置</Tag>}
              </div>
              <Form form={mappingForm} layout="vertical" disabled={!loaded || mappingSaving}>
                <Form.List name="mappings">
                  {(fields, { remove }) => <Space direction="vertical" size="small" style={{ width: '100%' }}>
                    {fields.map(field => <Row key={field.key} gutter={12} align="top">
                      <Col xs={24} md={5}>
                        <Form.Item name={[field.name, 'name']} label="名称"
                                   rules={[
                                     { required: true, message: '请输入名称' },
                                     { pattern: /^[a-z][a-z0-9-]{0,31}$/, message: '小写字母开头，仅限小写字母、数字和短横线' },
                                   ]}>
                          <Input placeholder="console" maxLength={32}/>
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={3}>
                        <Form.Item name={[field.name, 'mode']} label="方式" rules={[{ required: true }]}>
                          <Select options={[{ value: 'SYMLINK', label: '符号链接' }]}/>
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={5}>
                        <Form.Item name={[field.name, 'source']} label="发行源"
                                   rules={[
                                     { required: true, message: '请输入发行源' },
                                     {
                                       pattern: /^(console|desktop)(\/[A-Za-z0-9_@+.,-]+)*$/,
                                       message: '只能是 console、desktop 或其子路径'
                                     },
                                   ]}>
                          <Input placeholder="console"/>
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={9}>
                        <Form.Item name={[field.name, 'target']} label="Nginx 目标路径"
                                   rules={[
                                     { required: true, message: '请输入目标路径' },
                                     { pattern: /^\/[^\t\n\r]+$/, message: '请输入规范化的绝对路径' },
                                   ]}>
                          <Input placeholder="/var/www/today-blog/console"/>
                        </Form.Item>
                      </Col>
                      <Col xs={24} md={2}>
                        <Form.Item label=" ">
                          <Button danger type="text" icon={<MinusCircleOutlined/>}
                                  onClick={() => remove(field.name)}/>
                        </Form.Item>
                      </Col>
                    </Row>)}
                    {!fields.length && <Alert type="info" showIcon message="尚未配置映射，前端页面需要由 nginx 直接引用版本目录"/>}
                    <Button type="dashed" block icon={<PlusOutlined/>} onClick={addMapping}>添加映射</Button>
                  </Space>}
                </Form.List>
              </Form>
              <span className="upgrade-secondary">映射由管理端保存到数据库；启动升级任务时会快照，之后修改不影响已提交任务。</span>
            </Space>
          </>,
        },
      ]}/>
    </Modal>
    <Modal title="上传升级包" open={uploadVisible} confirmLoading={uploading} okText="上传"
           onOk={uploadPackage} onCancel={() => { setUploadVisible(false); setUploadFile(undefined); }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" showIcon message="仅支持完整发行 ZIP，最大 512 MiB"
               description="摘要只校验完整性，请仅上传自己构建并通过可信渠道传输的升级包。"/>
        <Upload accept=".zip" showUploadList={false} maxCount={1}
                beforeUpload={file => { setUploadFile(file); return false; }}>
          <Button icon={<UploadOutlined/>}>选择 ZIP 文件</Button>
        </Upload>
        <span>{uploadFile ? uploadFile.name : '尚未选择文件'}</span>
      </Space>
    </Modal>
    <Modal title="编辑升级包" open={Boolean(editPackage)} confirmLoading={packageSaving} okText="保存"
           onOk={savePackage} onCancel={() => setEditPackage(undefined)}>
      <Form layout="vertical">
        <Form.Item label="展示名称" required>
          <Input value={editName} maxLength={255} onChange={event => setEditName(event.target.value)}/>
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea value={editNote} maxLength={500} showCount rows={3}
                          onChange={event => setEditNote(event.target.value)}/>
        </Form.Item>
      </Form>
    </Modal>
    <Modal
        title={startPackage ? `升级到 ${startPackage.version} / ${startPackage.build}` : '开始升级'}
        open={Boolean(startPackage)}
        forceRender
        confirmLoading={starting}
        okText="开始升级"
        okButtonProps={{ disabled: !environmentReady }}
        onOk={start}
        onCancel={() => setStartPackage(undefined)}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" showIcon
               message={startPackage?.displayName || startPackage?.originalName}
               description={`当前版本 ${version.version} / ${version.build || '—'}；提交后立即进入维护状态。`}/>
        <Form form={runForm} layout="vertical" disabled={starting || !loaded}>
          <OptionFields/>
          <Form.Item name="note" label="本次升级备注"><Input.TextArea maxLength={500} showCount rows={2}/></Form.Item>
        </Form>
        <Checkbox checked={deletePackageAfter} disabled={starting}
                  onChange={event => setDeletePackageAfter(event.target.checked)}>
          升级成功后删除升级包
        </Checkbox>
      </Space>
    </Modal>
    <Modal
        title="开启维护状态"
        open={maintenanceVisible}
        confirmLoading={maintenanceSaving}
        okText="开启维护"
        onOk={enableMaintenance}
        onCancel={() => setMaintenanceVisible(false)}
    >
      <Alert type="info" showIcon message="开启后新的业务请求将返回 503"
             description="API 进程不会退出；完成后可在这里关闭，或等待升级流程自动关闭。"
             style={{ marginBottom: 16 }}/>
      <Form form={maintenanceForm} layout="vertical">
        <Form.Item name="message" label="提示信息" rules={[{ required: true, message: '请输入维护提示' }]}>
          <Input.TextArea maxLength={200} showCount rows={2}/>
        </Form.Item>
        <Form.Item name="retryAfter" label="建议重试间隔（秒）" rules={[{ required: true }]}>
          <InputNumber min={1} max={86400} precision={0} style={{ width: '100%' }}/>
        </Form.Item>
      </Form>
    </Modal>
  </PageContainer>;
}
