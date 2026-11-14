import React, { useCallback, useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
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
  Table,
  Tabs,
  Tag,
  Upload
} from 'antd';
import { MinusCircleOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { http } from '@/utils/http';
import './index.less';

interface UpgradeTask {
  id: string;
  version: string;
  build: string;
  status: string;
  message: string;

  [key: string]: string;
}

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

interface MaintenanceStatus {
  enabled: boolean;
  message?: string;
  retryAfterSeconds?: number;
  source?: 'upgrade' | 'manual';
}

const terminal = ['SUCCESS', 'FAILED', 'ROLLED_BACK'];
const taskKey = 'today-blog-upgrade-task';
const phases = ['VALIDATING', 'INSTALLING', 'BACKING_UP', 'RESTARTING', 'HEALTH_CHECKING', 'SUCCESS'];
const labels: Record<string, string> = {
  READY: '待确认', STARTING: '已提交', VALIDATING: '校验升级包', INSTALLING: '准备版本',
  BACKING_UP: '数据库备份', RESTARTING: '切换并重启', HEALTH_CHECKING: '健康检查',
  SUCCESS: '更新成功', FAILED: '更新失败', ROLLING_BACK: '正在恢复旧程序', ROLLED_BACK: '更新失败，已恢复旧版本',
};

function parseOptions(data: Record<string, string>): Options {
  return {
    backupEnabled: data.backupEnabled === 'true',
    backupTimeoutSeconds: Number(data.backupTimeoutSeconds),
    healthTimeoutSeconds: Number(data.healthTimeoutSeconds),
    note: data.note || '',
  };
}

function requestOptions(options: Options) {
  return {
    backupEnabled: String(options.backupEnabled), backupTimeoutSeconds: String(options.backupTimeoutSeconds),
    healthTimeoutSeconds: String(options.healthTimeoutSeconds), note: options.note || '',
  };
}

function size(bytes: number) {
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GiB`;
}

function OptionFields() {
  return <>
    <Form.Item name="backupEnabled" label="更新前备份数据库" valuePropName="checked"
               extra="备份失败将终止更新；关闭后必须自行保证有可恢复的备份。">
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
  const [version, setVersion] = useState<Record<string, string>>({ version: '加载中' });
  const [task, setTask] = useState<UpgradeTask>();
  const [id, setId] = useState(() => localStorage.getItem(taskKey) || '');
  const [history, setHistory] = useState<UpgradeTask[]>([]);
  const [environment, setEnvironment] = useState<Environment>();
  const [uploading, setUploading] = useState(false);
  const [starting, setStarting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [tab, setTab] = useState('run');
  const [disconnected, setDisconnected] = useState(false);
  const [needsLogin, setNeedsLogin] = useState(false);
  const [maintenance, setMaintenance] = useState<MaintenanceStatus>();
  const [maintenanceSaving, setMaintenanceSaving] = useState(false);
  const [maintenanceVisible, setMaintenanceVisible] = useState(false);
  const [mappingSettings, setMappingSettings] = useState<MappingSettings>();
  const [mappingSaving, setMappingSaving] = useState(false);
  const [runForm] = Form.useForm<Options>();
  const [settingsForm] = Form.useForm<Options>();
  const [mappingForm] = Form.useForm<{ mappings: ReleaseMapping[] }>();
  const [maintenanceForm] = Form.useForm<{ message: string; retryAfter: number }>();
  const backupEnabled = Form.useWatch('backupEnabled', runForm);

  const selectTask = useCallback((next: UpgradeTask) => {
    setTask(next);
    setTab('run');
    setId(next.id);
    localStorage.setItem(taskKey, next.id);
  }, []);

  const refresh = useCallback(async () => {
    const [current, records, env, maintenanceStatus] = await Promise.all([
      http.get('/api/console/maintenance/upgrade/current'), http.get('/api/console/maintenance/upgrade/history'),
      http.get('/api/console/maintenance/upgrade/environment'), http.get('/api/console/maintenance'),
    ]);
    setVersion(current.data);
    setHistory(records.data);
    setEnvironment(env.data);
    setMaintenance(maintenanceStatus.data);
    return records.data as UpgradeTask[];
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [settings, mappings, records] = await Promise.all([
          http.get('/api/console/maintenance/upgrade/settings'),
          http.get('/api/console/maintenance/upgrade/mappings'),
          refresh(),
        ]);
        if (cancelled) {
          return;
        }
        const options = parseOptions(settings.data);
        runForm.setFieldsValue(options);
        settingsForm.setFieldsValue(options);
        setMappingSettings(mappings.data);
        mappingForm.setFieldsValue({ mappings: mappings.data.mappings || [] });
        const active = records.find(record => record.status !== 'READY' && !terminal.includes(record.status));
        if (active) {
          selectTask(active);
        }
        setLoaded(true);
      }
      catch (error) {
        message.error('无法加载更新环境，请刷新页面重试');
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [refresh, runForm, settingsForm, mappingForm, selectTask]);

  useEffect(() => {
    if (!id) {
      return undefined;
    }
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      let done = false;
      try {
        const response = await http.get(`/api/console/maintenance/upgrade/${id}`);
        if (cancelled) {
          return;
        }
        setTask(response.data);
        setDisconnected(false);
        setNeedsLogin(false);
        done = terminal.includes(response.data.status) || response.data.status === 'READY';
        if (done) {
          await refresh();
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
  }, [id, starting, refresh]);

  const activeTask = history.some(record => record.status !== 'READY' && !terminal.includes(record.status));
  const busy = uploading || starting || activeTask || Boolean(task && task.status !== 'READY' && !terminal.includes(task.status));
  const ready = loaded && environment?.updaterReady && environment.installFreeBytes >= 3 * 1024 ** 3
      && (!backupEnabled || (environment.backupReady && environment.backupFreeBytes >= 256 * 1024 ** 2));
  const lastPhase = task ? phases.reduce((last, phase, index) => task[`stage.${phase}`] ? index : last, 0) : 0;

  async function upload(file: File) {
    setUploading(true);
    try {
      const data = new FormData();
      data.append('file', file);
      const response = await http.post('/api/console/maintenance/upgrade/packages', data, { timeout: 300000 });
      selectTask(response.data);
      await refresh();
      message.success('升级包完整性校验通过，请确认来源可信');
    }
    catch (error) {
      // 通用 HTTP 拦截器负责错误提示。
    }
    finally {
      setUploading(false);
    }
    return false;
  }

  async function start() {
    const options = await runForm.validateFields();
    Modal.confirm({
      title: `确认更新到 ${task?.version} / ${task?.build}？`,
      content: <Space direction="vertical">
        <span>当前版本：{version.version} / {version.build || '—'}</span>
        <span>数据库备份：{options.backupEnabled ? `开启，超时 ${options.backupTimeoutSeconds} 秒` : '关闭，请确认已自行备份'}</span>
        <span>健康检查超时：{options.healthTimeoutSeconds} 秒。</span>
        <span>文件映射：{mappingSettings?.mappings?.length
            ? `${mappingSettings.mappings.length} 条，切换版本后发布`
            : '未配置'}</span>
        <span>提交后立即进入维护状态，新的业务请求返回 503；升级完成后自动恢复。</span>
        {maintenance?.enabled && maintenance.source === 'manual'
            && <span>当前为手动维护状态，升级完成后不会自动关闭，请升级完成后手动关闭。</span>}
        <span>服务会短暂中断。仅适用于向后兼容的数据库迁移；自动回滚只恢复程序，不恢复数据库。</span>
      </Space>,
      okText: options.backupEnabled ? '开始更新' : '确认跳过备份并更新',
      okButtonProps: { danger: !options.backupEnabled },
      async onOk() {
        setStarting(true);
        try {
          await http.post(`/api/console/maintenance/upgrade/${id}/start`, requestOptions(options));
          setTask(previous => previous && ({ ...previous, status: 'STARTING', message: '更新任务已提交' }));
          await refresh();
        }
        finally {
          setStarting(false);
        }
      },
    });
  }

  async function saveSettings() {
    const options = await settingsForm.validateFields();
    setSaving(true);
    try {
      await http.put('/api/console/maintenance/upgrade/settings', requestOptions({ ...options, note: '' }));
      const response = await http.get('/api/console/maintenance/upgrade/settings');
      settingsForm.setFieldsValue(parseOptions(response.data));
      message.success('默认设置已保存；本次更新参数保持不变');
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

  return <PageContainer className="system-update-page" title="系统更新" subTitle="发布新版本，保留可追溯的备份与更新记录">
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message="程序回滚不等于数据库恢复"
             description="仅发布数据库迁移向后兼容的版本。在线备份针对 InnoDB，不包含附件，也不包含备份时间点之后的新数据；更新期间不得并发修改表结构。"/>
      {maintenance?.enabled && <Alert type="warning" showIcon message="维护状态已开启"
                                      description={`${maintenance.message || '系统维护中，请稍后重试'}；建议重试间隔 ${maintenance.retryAfterSeconds || 120} 秒。${maintenanceSource}。`}/>}
      {disconnected && <Alert type="info" showIcon message={needsLogin ? '登录已失效，请重新登录后查看任务' : '连接暂时中断，正在自动重试'}
                              description="任务在服务器独立运行，请勿重复提交。长时间未恢复时请通过 SSH 检查 systemd 日志。"/>}
      <Tabs activeKey={tab} onChange={setTab} items={[
        {
          key: 'run', label: '执行更新', children: <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Card title="当前环境" extra={<Space>
              {maintenance?.enabled
                  ? <Button danger loading={maintenanceSaving} disabled={busy || !loaded} onClick={disableMaintenance}>关闭维护</Button>
                  : <Button loading={maintenanceSaving} disabled={busy || !loaded} onClick={openMaintenanceModal}>开启维护</Button>}
              <Button onClick={() => refresh().catch(() => message.error('刷新环境失败'))}>刷新状态</Button>
            </Space>}>
              <Descriptions column={{ xs: 1, sm: 2, lg: 3 }}>
                <Descriptions.Item label="当前版本">{version.version} / {version.build || '—'}</Descriptions.Item>
                <Descriptions.Item label="维护状态">
                  <Tag color={maintenance?.enabled ? 'warning' : 'success'}>
                    {maintenance?.enabled ? (maintenance.source === 'manual' ? '手动维护中' : '升级维护中') : '正常服务'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="升级器"><Tag
                    color={environment?.updaterReady ? 'success' : 'warning'}>{environment?.updaterReady ? '已配置' : '未就绪'}</Tag></Descriptions.Item>
                <Descriptions.Item label="数据库备份"><Tag
                    color={environment?.backupReady ? 'success' : 'warning'}>{environment?.backupReady ? '已配置，执行时验证连接' : '未就绪'}</Tag></Descriptions.Item>
                <Descriptions.Item label="安装盘可用空间">{environment ? size(environment.installFreeBytes) : '—'}（至少 3
                  GiB）</Descriptions.Item>
                <Descriptions.Item label="备份盘可用空间">{environment ? size(environment.backupFreeBytes) : '—'}（至少 256
                  MiB，另需足够导出空间）</Descriptions.Item>
              </Descriptions>
            </Card>
            <Card title="安装升级包">
              <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                <Upload accept=".zip" showUploadList={false} beforeUpload={upload} disabled={busy || !loaded || !environment?.updaterReady}>
                  <Button icon={<UploadOutlined/>} loading={uploading}>选择并上传升级包</Button>
                </Upload>
                <span>仅支持完整发行 ZIP，最大 512 MiB。摘要只校验完整性，请仅上传自己构建的可信包。</span>
                {task?.status === 'READY' && <Alert type="success" showIcon message={`待更新：${task.version} / ${task.build}`}
                                                    description={`包大小：${(Number(task.packageBytes || 0) / 1024 / 1024).toFixed(1)} MiB，完整性校验通过。`}/>}
                <Form form={runForm} layout="vertical" disabled={busy || !loaded}>
                  <OptionFields/>
                  <Form.Item name="note" label="本次更新备注"><Input.TextArea maxLength={500} showCount rows={2}/></Form.Item>
                </Form>
                <Button type="primary" disabled={task?.status !== 'READY' || busy || !ready} loading={starting}
                        onClick={start}>开始更新</Button>
              </Space>
            </Card>
            {task && <Card title="更新任务">
              <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Alert showIcon
                       type={task.status === 'SUCCESS' ? 'success' : ['FAILED', 'ROLLED_BACK'].includes(task.status) ? 'error' : 'info'}
                       message={labels[task.status] || task.status} description={task.message}/>
                <Steps size="small" responsive current={phases.includes(task.status) ? phases.indexOf(task.status) : lastPhase}
                       status={['FAILED', 'ROLLED_BACK'].includes(task.status) ? 'error' : undefined}
                       items={phases.map(phase => ({ title: labels[phase], description: task[`stage.${phase}`] ? '已进入' : undefined }))}/>
                <Descriptions column={{ xs: 1, sm: 2 }}>
                  <Descriptions.Item label="目标版本">{task.version} / {task.build}</Descriptions.Item>
                  <Descriptions.Item label="任务 ID">{task.id}</Descriptions.Item>
                  <Descriptions.Item
                    label="数据库备份">{task.backupStatus || '尚未执行'}{task.backupBytes ? ` / ${(Number(task.backupBytes) / 1024 / 1024).toFixed(1)} MiB` : ''}</Descriptions.Item>
                  <Descriptions.Item label="备份标识">{task.backupId || '—'}</Descriptions.Item>
                  <Descriptions.Item label="开始时间">{task.startedAt || '—'}</Descriptions.Item>
                  <Descriptions.Item label="结束时间">{task.finishedAt || '—'}</Descriptions.Item>
                  <Descriptions.Item label="备注">{task.note || '—'}</Descriptions.Item>
                </Descriptions>
                {task.status === 'SUCCESS' && <Button onClick={() => window.location.reload()}>刷新并使用新版本</Button>}
              </Space>
            </Card>}
          </Space>
        },
        {
          key: 'settings', label: '更新设置', forceRender: true, children: <Card title="默认更新参数">
            <Alert type="info" showIcon
                   message="默认设置仅用于以后载入页面时初始化参数，不影响已提交的任务。映射目标白名单、命令及数据库凭据由运维配置。"
                   style={{ marginBottom: 24 }}/>
            <Form form={settingsForm} layout="vertical" disabled={!loaded || saving} style={{ maxWidth: 600 }}>
              <OptionFields/>
              <Button type="primary" onClick={saveSettings} loading={saving}
                      disabled={!loaded || !environment?.updaterReady}>保存默认设置</Button>
            </Form>
          </Card>
        },
        {
          key: 'mappings',
          label: '文件映射',
          forceRender: true,
          children: <Card title="前端文件映射"
                          extra={<Space>
                            <Button onClick={() => reloadMappings().catch(() => message.error('重新载入映射失败'))}>重新载入</Button>
                            <Button type="primary" loading={mappingSaving}
                                    disabled={!loaded || !mappingSettings?.ready || !environment?.updaterReady}
                                    onClick={saveMappings}>保存映射</Button>
                          </Space>}>
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Alert type="info" showIcon
                     message="映射在升级切换版本后发布为指向 current/<source> 的符号链接，回滚时自动指回旧版本。"
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
                    {!fields.length && <Alert type="info" showIcon message="尚未配置映射，前端页面需要由 nginx 直接引用版本目录"/>
                    }
                    <Button type="dashed" block icon={<PlusOutlined/>} onClick={addMapping}>添加映射</Button>
                  </Space>}
                </Form.List>
              </Form>
              <span>映射由管理端保存到数据库；启动升级任务时会快照，之后修改不影响已提交任务。</span>
            </Space>
          </Card>
        },
        {
          key: 'history',
          label: '更新记录',
          children: <Card title="历史任务"
                          extra={<Button onClick={() => refresh().catch(() => message.error('刷新记录失败'))}>刷新记录</Button>}>
            <Table<UpgradeTask> rowKey="id" dataSource={history} scroll={{ x: 1000 }} pagination={{ pageSize: 10 }} columns={[
              { title: '目标版本', key: 'version', render: (_, record) => `${record.version} / ${record.build}` },
              { title: '状态', dataIndex: 'status', render: value => labels[value] || value },
              { title: '操作人 ID', dataIndex: 'operatorId' },
              { title: '开始时间', dataIndex: 'startedAt' },
              { title: '结束时间', dataIndex: 'finishedAt' },
              {
                title: '操作',
                key: 'action',
                render: (_, record) => <Button type="link" onClick={() => selectTask(record)}>查看详情</Button>
              },
            ]} expandable={{
              expandedRowRender: record => <Descriptions column={1}>
                <Descriptions.Item label="任务 ID">{record.id}</Descriptions.Item>
                <Descriptions.Item label="原版本">{record.oldVersion || '—'} / {record.oldBuild || '—'}</Descriptions.Item>
                <Descriptions.Item label="详情">{record.message}</Descriptions.Item>
                <Descriptions.Item
                    label="参数快照">备份：{record.backupEnabled || '—'}；备份超时：{record.backupTimeoutSeconds || '—'} 秒；健康检查：{record.healthTimeoutSeconds || '—'} 秒</Descriptions.Item>
                <Descriptions.Item
                    label="备份">{record.backupStatus || '—'} / {record.backupId || '—'} / {record.backupCompletedAt || '—'}</Descriptions.Item>
                <Descriptions.Item
                    label="阶段时间">{Object.entries(record).filter(([key]) => key.startsWith('stage.')).map(([key, value]) => <div
                    key={key}>{labels[key.substring(6)] || key.substring(6)}：{value}</div>)}</Descriptions.Item>
                <Descriptions.Item label="备注">{record.note || '—'}</Descriptions.Item>
              </Descriptions>
            }}/>
          </Card>
        },
      ]}/>
    </Space>
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
