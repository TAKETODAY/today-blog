import React, { useCallback, useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Alert, Button, Card, Descriptions, Form, Input, InputNumber, message, Modal, Space, Steps, Switch, Table, Tabs, Tag, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
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
    <Form.Item name="backupEnabled" label="更新前备份数据库" valuePropName="checked" extra="备份失败将终止更新；关闭后必须自行保证有可恢复的备份。">
      <Switch checkedChildren="开启" unCheckedChildren="关闭"/>
    </Form.Item>
    <Form.Item name="backupTimeoutSeconds" label="备份超时（秒）" rules={[{required: true}]}>
      <InputNumber min={30} max={3600} precision={0}/>
    </Form.Item>
    <Form.Item name="healthTimeoutSeconds" label="健康检查超时（秒）" rules={[{required: true}]}>
      <InputNumber min={10} max={600} precision={0}/>
    </Form.Item>
  </>;
}

export default function SystemUpgrade() {
  const [version, setVersion] = useState<Record<string, string>>({version: '加载中'});
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
  const [runForm] = Form.useForm<Options>();
  const [settingsForm] = Form.useForm<Options>();
  const backupEnabled = Form.useWatch('backupEnabled', runForm);

  const selectTask = useCallback((next: UpgradeTask) => {
    setTask(next);
    setTab('run');
    setId(next.id);
    localStorage.setItem(taskKey, next.id);
  }, []);

  const refresh = useCallback(async () => {
    const [current, records, env] = await Promise.all([
      http.get('/api/console/upgrade/current'), http.get('/api/console/upgrade/history'), http.get('/api/console/upgrade/environment'),
    ]);
    setVersion(current.data);
    setHistory(records.data);
    setEnvironment(env.data);
    return records.data as UpgradeTask[];
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [settings, records] = await Promise.all([http.get('/api/console/upgrade/settings'), refresh()]);
        if (cancelled) {
          return;
        }
        const options = parseOptions(settings.data);
        runForm.setFieldsValue(options);
        settingsForm.setFieldsValue(options);
        const active = records.find(record => record.status !== 'READY' && !terminal.includes(record.status));
        if (active) {
          selectTask(active);
        }
        setLoaded(true);
      } catch (error) {
        message.error('无法加载更新环境，请刷新页面重试');
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [refresh, runForm, settingsForm, selectTask]);

  useEffect(() => {
    if (!id) {
      return undefined;
    }
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      let done = false;
      try {
        const response = await http.get(`/api/console/upgrade/${id}`);
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
      } catch (error) {
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
      const response = await http.post('/api/console/upgrade/packages', data, {timeout: 300000});
      selectTask(response.data);
      await refresh();
      message.success('升级包完整性校验通过，请确认来源可信');
    } catch (error) {
      // 通用 HTTP 拦截器负责错误提示。
    } finally {
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
        <span>服务会短暂中断。仅适用于向后兼容的数据库迁移；自动回滚只恢复程序，不恢复数据库。</span>
      </Space>,
      okText: options.backupEnabled ? '开始更新' : '确认跳过备份并更新',
      okButtonProps: {danger: !options.backupEnabled},
      async onOk() {
        setStarting(true);
        try {
          await http.post(`/api/console/upgrade/${id}/start`, requestOptions(options));
          setTask(previous => previous && ({...previous, status: 'STARTING', message: '更新任务已提交'}));
        } finally {
          setStarting(false);
        }
      },
    });
  }

  async function saveSettings() {
    const options = await settingsForm.validateFields();
    setSaving(true);
    try {
      await http.put('/api/console/upgrade/settings', requestOptions({...options, note: ''}));
      const response = await http.get('/api/console/upgrade/settings');
      settingsForm.setFieldsValue(parseOptions(response.data));
      message.success('默认设置已保存；本次更新参数保持不变');
    } finally {
      setSaving(false);
    }
  }

  return <PageContainer className="system-update-page" title="系统更新" subTitle="发布新版本，保留可追溯的备份与更新记录">
    <Space direction="vertical" size="large" style={{width: '100%'}}>
      <Alert type="warning" showIcon message="程序回滚不等于数据库恢复"
             description="仅发布数据库迁移向后兼容的版本。在线备份针对 InnoDB，不包含附件，也不包含备份时间点之后的新数据；更新期间不得并发修改表结构。"/>
      {disconnected && <Alert type="info" showIcon message={needsLogin ? '登录已失效，请重新登录后查看任务' : '连接暂时中断，正在自动重试'}
                              description="任务在服务器独立运行，请勿重复提交。长时间未恢复时请通过 SSH 检查 systemd 日志。"/>}
      <Tabs activeKey={tab} onChange={setTab} items={[
        {
          key: 'run', label: '执行更新', children: <Space direction="vertical" size="large" style={{width: '100%'}}>
            <Card title="当前环境" extra={<Button onClick={() => refresh().catch(() => message.error('刷新环境失败'))}>刷新状态</Button>}>
              <Descriptions column={{xs: 1, sm: 2, lg: 3}}>
                <Descriptions.Item label="当前版本">{version.version} / {version.build || '—'}</Descriptions.Item>
                <Descriptions.Item label="升级器"><Tag color={environment?.updaterReady ? 'success' : 'warning'}>{environment?.updaterReady ? '已配置' : '未就绪'}</Tag></Descriptions.Item>
                <Descriptions.Item label="数据库备份"><Tag color={environment?.backupReady ? 'success' : 'warning'}>{environment?.backupReady ? '已配置，执行时验证连接' : '未就绪'}</Tag></Descriptions.Item>
                <Descriptions.Item label="安装盘可用空间">{environment ? size(environment.installFreeBytes) : '—'}（至少 3 GiB）</Descriptions.Item>
                <Descriptions.Item label="备份盘可用空间">{environment ? size(environment.backupFreeBytes) : '—'}（至少 256 MiB，另需足够导出空间）</Descriptions.Item>
              </Descriptions>
            </Card>
            <Card title="安装升级包">
              <Space direction="vertical" size="middle" style={{width: '100%'}}>
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
                <Button type="primary" disabled={task?.status !== 'READY' || busy || !ready} loading={starting} onClick={start}>开始更新</Button>
              </Space>
            </Card>
            {task && <Card title="更新任务">
              <Space direction="vertical" size="large" style={{width: '100%'}}>
                <Alert showIcon type={task.status === 'SUCCESS' ? 'success' : ['FAILED', 'ROLLED_BACK'].includes(task.status) ? 'error' : 'info'}
                       message={labels[task.status] || task.status} description={task.message}/>
                <Steps size="small" responsive current={phases.includes(task.status) ? phases.indexOf(task.status) : lastPhase}
                       status={['FAILED', 'ROLLED_BACK'].includes(task.status) ? 'error' : undefined}
                       items={phases.map(phase => ({title: labels[phase], description: task[`stage.${phase}`] ? '已进入' : undefined}))}/>
                <Descriptions column={{xs: 1, sm: 2}}>
                  <Descriptions.Item label="目标版本">{task.version} / {task.build}</Descriptions.Item>
                  <Descriptions.Item label="任务 ID">{task.id}</Descriptions.Item>
                  <Descriptions.Item label="数据库备份">{task.backupStatus || '尚未执行'}{task.backupBytes ? ` / ${(Number(task.backupBytes) / 1024 / 1024).toFixed(1)} MiB` : ''}</Descriptions.Item>
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
            <Alert type="info" showIcon message="默认设置仅用于以后载入页面时初始化参数，不影响已提交的任务。服务器路径、命令及数据库凭据由运维配置。" style={{marginBottom: 24}}/>
            <Form form={settingsForm} layout="vertical" disabled={!loaded || saving} style={{maxWidth: 600}}>
              <OptionFields/>
              <Button type="primary" onClick={saveSettings} loading={saving} disabled={!loaded || !environment?.updaterReady}>保存默认设置</Button>
            </Form>
          </Card>
        },
        {
          key: 'history', label: '更新记录', children: <Card title="历史任务" extra={<Button onClick={() => refresh().catch(() => message.error('刷新记录失败'))}>刷新记录</Button>}>
            <Table<UpgradeTask> rowKey="id" dataSource={history} scroll={{x: 1000}} pagination={{pageSize: 10}} columns={[
              {title: '目标版本', key: 'version', render: (_, record) => `${record.version} / ${record.build}`},
              {title: '状态', dataIndex: 'status', render: value => labels[value] || value},
              {title: '操作人 ID', dataIndex: 'operatorId'},
              {title: '开始时间', dataIndex: 'startedAt'},
              {title: '结束时间', dataIndex: 'finishedAt'},
              {title: '操作', key: 'action', render: (_, record) => <Button type="link" onClick={() => selectTask(record)}>查看详情</Button>},
            ]} expandable={{
              expandedRowRender: record => <Descriptions column={1}>
                <Descriptions.Item label="任务 ID">{record.id}</Descriptions.Item>
                <Descriptions.Item label="原版本">{record.oldVersion || '—'} / {record.oldBuild || '—'}</Descriptions.Item>
                <Descriptions.Item label="详情">{record.message}</Descriptions.Item>
                <Descriptions.Item label="参数快照">备份：{record.backupEnabled || '—'}；备份超时：{record.backupTimeoutSeconds || '—'} 秒；健康检查：{record.healthTimeoutSeconds || '—'} 秒</Descriptions.Item>
                <Descriptions.Item label="备份">{record.backupStatus || '—'} / {record.backupId || '—'} / {record.backupCompletedAt || '—'}</Descriptions.Item>
                <Descriptions.Item label="阶段时间">{Object.entries(record).filter(([key]) => key.startsWith('stage.')).map(([key, value]) => <div key={key}>{labels[key.substring(6)] || key.substring(6)}：{value}</div>)}</Descriptions.Item>
                <Descriptions.Item label="备注">{record.note || '—'}</Descriptions.Item>
              </Descriptions>
            }}/>
          </Card>
        },
      ]}/>
    </Space>
  </PageContainer>;
}
