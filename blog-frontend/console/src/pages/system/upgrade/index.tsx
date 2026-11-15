import React, { useCallback, useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Alert, Button, Card, message, Modal, Space, Tabs } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { http } from '@/utils/http';
import type { Environment, Options, UpgradePackage, UpgradeRecord, VersionInfo } from './types';
import { parseOptions, taskKey, terminal } from './utils';
import { useMaintenance } from './useMaintenance';
import { useUpgradeSettings } from './useUpgradeSettings';
import UpgradeHeaderExtra from './UpgradeHeaderExtra';
import UpgradePackageTable from './UpgradePackageTable';
import UpgradeTaskProgress from './UpgradeTaskProgress';
import UpgradeHistoryTable from './UpgradeHistoryTable';
import UpgradeConfigModal from './UpgradeConfigModal';
import UploadPackageModal from './UploadPackageModal';
import EditPackageModal from './EditPackageModal';
import StartUpgradeModal from './StartUpgradeModal';
import MaintenanceModal from './MaintenanceModal';
import './index.less';

type Dialog =
    | { type: 'upload' }
    | { type: 'edit'; item: UpgradePackage }
    | { type: 'start'; item: UpgradePackage };

interface ConfigState {
  visible: boolean;
  tab: string;
}

export default function SystemUpgrade() {
  const [version, setVersion] = useState<VersionInfo>({ version: '加载中' });
  const [record, setRecord] = useState<UpgradeRecord>();
  const [id, setId] = useState(() => localStorage.getItem(taskKey) || '');
  const [records, setRecords] = useState<UpgradeRecord[]>([]);
  const [packages, setPackages] = useState<UpgradePackage[]>([]);
  const [environment, setEnvironment] = useState<Environment>();
  const [refreshing, setRefreshing] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [tab, setTab] = useState('run');
  const [disconnected, setDisconnected] = useState(false);
  const [needsLogin, setNeedsLogin] = useState(false);
  const [options, setOptions] = useState<Options>(() => parseOptions());
  const [dialog, setDialog] = useState<Dialog>();
  const [config, setConfig] = useState<ConfigState>({ visible: false, tab: 'settings' });
  const maintenance = useMaintenance();
  const settings = useUpgradeSettings();
  const { apply: applySettings } = settings;
  const { setStatus: setMaintenanceStatus } = maintenance;

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
      setMaintenanceStatus(maintenanceStatus.data);
      return history.data as UpgradeRecord[];
    }
    finally {
      setRefreshing(false);
    }
  }, [setMaintenanceStatus]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [settingsResponse, mappingsResponse, history] = await Promise.all([
          http.get('/api/console/maintenance/upgrade/settings'),
          http.get('/api/console/maintenance/upgrade/mappings'),
          refresh(),
          reloadPackages(),
        ]);
        if (cancelled) {
          return;
        }
        const defaults = parseOptions(settingsResponse.data);
        setOptions(defaults);
        applySettings(defaults, mappingsResponse.data);
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
  }, [refresh, reloadPackages, applySettings, selectRecord]);

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
  }, [id, refresh, reloadPackages]);

  const busy = records.some(item => !terminal.includes(item.status));

  async function removePackage(item: UpgradePackage) {
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

  const handleStarted = useCallback(async (next: UpgradeRecord) => {
    setDialog(undefined);
    selectRecord(next);
    await refresh();
    await reloadPackages();
  }, [refresh, reloadPackages, selectRecord]);

  function openConfig(tabKey?: string) {
    setConfig(prev => ({ visible: true, tab: tabKey || prev.tab }));
  }

  const mappingSettings = settings.mappingSettings;

  return <PageContainer className="system-update-page" title="系统升级" subTitle="发布新版本，保留可追溯的备份与升级记录"
                        extra={<UpgradeHeaderExtra
                            version={version}
                            environment={environment}
                            maintenance={maintenance}
                            busy={busy}
                            loaded={loaded}
                            refreshing={refreshing}
                            mappingPending={Boolean(mappingSettings && !mappingSettings.ready)}
                            onRefresh={() => refresh().catch(() => message.error('刷新环境失败'))}
                            onOpenConfig={() => openConfig()}/>}>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Alert type="warning" showIcon message="程序回滚不等于数据库恢复"
             description="仅发布数据库迁移向后兼容的版本。在线备份针对 InnoDB，不包含附件，也不包含备份时间点之后的新数据；升级期间不得并发修改表结构。"/>
      {maintenance.status?.enabled && <Alert type="warning" showIcon message="维护状态已开启"
                                             description={`${maintenance.status.message || '系统维护中，请稍后重试'}；建议重试间隔 ${maintenance.status.retryAfterSeconds || 120} 秒。${maintenance.sourceLabel}。`}/>}
      {disconnected && <Alert type="info" showIcon message={needsLogin ? '登录已失效，请重新登录后查看任务' : '连接暂时中断，正在自动重试'}
                              description="任务在服务器独立运行，请勿重复提交。长时间未恢复时请通过 SSH 检查 systemd 日志。"/>}
      <Card>
        <Tabs activeKey={tab} onChange={setTab}
              tabBarExtraContent={tab === 'run'
                  ? <Button icon={<UploadOutlined/>} onClick={() => setDialog({ type: 'upload' })}
                            disabled={!loaded}>上传升级包</Button>
                  : undefined}
              items={[
                {
                  key: 'run',
                  label: '执行升级',
                  children:
                      <Space direction="vertical" size="large" style={{ width: '100%' }}>
                        {loaded && mappingSettings && !mappingSettings.ready && <Alert type="warning" showIcon
                                                                                       message="文件映射配置未就绪"
                                                                                       description={mappingSettings.message || '升级时将无法发布前端文件，请检查服务器目标根目录配置。'}
                                                                                       action={<Button size="small"
                                                                                                       onClick={() => openConfig('mappings')}>去配置</Button>}/>}
                        <div>
                          <UpgradePackageTable
                              packages={packages}
                              busy={busy}
                              onStart={item => setDialog({ type: 'start', item })}
                              onEdit={item => setDialog({ type: 'edit', item })}
                              onRemove={removePackage}/>
                        </div>
                        {record && <UpgradeTaskProgress record={record}/>}
                      </Space>,
                },
                {
                  key: 'history',
                  label: '升级记录',
                  children: <UpgradeHistoryTable
                      records={records}
                      loading={refreshing}
                      onRefresh={() => refresh().catch(() => message.error('刷新记录失败'))}
                      onSelect={selectRecord}/>,
                },
              ]}/>
      </Card>
    </Space>
    <UpgradeConfigModal
        open={config.visible}
        activeTab={config.tab}
        loaded={loaded}
        updaterReady={environment?.updaterReady}
        settings={settings}
        onTabChange={next => setConfig(prev => ({ ...prev, tab: next }))}
        onClose={() => setConfig(prev => ({ ...prev, visible: false }))}/>
    <UploadPackageModal
        open={dialog?.type === 'upload'}
        onClose={() => setDialog(undefined)}
        onUploaded={reloadPackages}/>
    <EditPackageModal
        item={dialog?.type === 'edit' ? dialog.item : undefined}
        onClose={() => setDialog(undefined)}
        onSaved={reloadPackages}/>
    <StartUpgradeModal
        item={dialog?.type === 'start' ? dialog.item : undefined}
        version={version}
        environment={environment}
        loaded={loaded}
        options={options}
        onClose={() => setDialog(undefined)}
        onStarted={handleStarted}/>
    <MaintenanceModal maintenance={maintenance}/>
  </PageContainer>;
}
