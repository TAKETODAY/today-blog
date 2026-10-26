import React, { useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Alert, Button, Card, Descriptions, message, Modal, Space, Tag, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { http } from '@/utils/http';

interface UpgradeTask {
  id: string;
  version: string;
  build: string;
  status: string;
  message: string;
  updatedAt?: string;
}

const terminal = ['SUCCESS', 'FAILED', 'ROLLED_BACK'];
const taskKey = 'today-blog-upgrade-task';

export default function SystemUpgrade() {
  const [version, setVersion] = useState('加载中');
  const [task, setTask] = useState<UpgradeTask>();
  const [id, setId] = useState(() => localStorage.getItem(taskKey) || '');
  const [uploading, setUploading] = useState(false);
  const [starting, setStarting] = useState(false);
  const [disconnected, setDisconnected] = useState(false);

  useEffect(() => {
    http.get('/api/console/upgrade/current').then(response => setVersion(response.data.version)).catch(() => setVersion('无法读取'));
  }, []);

  useEffect(() => {
    if (!id) return undefined;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      let done = false;
      try {
        const response = await http.get(`/api/console/upgrade/${id}`);
        if (cancelled) return;
        setTask(response.data);
        setDisconnected(false);
        done = terminal.includes(response.data.status) || response.data.status === 'READY';
      }
      catch (error) {
        if (!cancelled) {
          setDisconnected(true);
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
  }, [id, starting]);

  const busy = uploading || starting || Boolean(task && task.status !== 'READY' && !terminal.includes(task.status));

  async function upload(file: File) {
    setUploading(true);
    try {
      const data = new FormData();
      data.append('file', file);
      const response = await http.post('/api/console/upgrade/packages', data, { timeout: 300000 });
      setTask(response.data);
      setId(response.data.id);
      localStorage.setItem(taskKey, response.data.id);
      message.success('升级包签名与文件校验通过');
    }
    catch (error) {
      /* 通用 HTTP 拦截器负责错误提示。 */
    }
    finally {
      setUploading(false);
    }
    return false;
  }

  function start() {
    Modal.confirm({
      title: `确认升级到 ${task?.version}？`,
      content: '服务会短暂中断。请先备份数据库；自动回滚仅恢复程序，不撤销数据库迁移。',
      okText: '开始升级',
      async onOk() {
        setStarting(true);
        try {
          await http.post(`/api/console/upgrade/${id}/start`);
          setTask(previous => previous && ({ ...previous, status: 'STARTING', message: '升级任务已提交' }));
        }
        finally {
          setStarting(false);
        }
      },
    });
  }

  return (
      <PageContainer title="系统升级" subTitle="上传完整签名发行包，确认后开始升级">
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Alert type="warning" showIcon message="升级前请备份数据库"
                 description="升级不覆盖附件、生产配置或日志。服务重启期间连接暂时中断，页面会继续查询；请勿重复发起任务。"/>
          <Card title="安装升级包" extra={<Tag color="#337ab7">当前版本：{version}</Tag>}>
            <Space>
              <Upload accept=".zip" showUploadList={false} beforeUpload={upload} disabled={busy}>
                <Button icon={<UploadOutlined/>} loading={uploading} disabled={busy}>选择并上传升级包</Button>
              </Upload>
              <Button type="primary" disabled={task?.status !== 'READY' || busy} onClick={start}>开始升级</Button>
            </Space>
            <p style={{ color: '#666', marginTop: 16 }}>仅支持 TODAY BLOG 签名 ZIP，最大 512 MiB。私钥不应上传至服务器。</p>
          </Card>
          {disconnected &&
            <Alert type="info" showIcon message="暂时无法连接，正在重试"
                   description="若长时间未恢复，请通过 SSH 查看 systemd 日志和升级状态。"/>}
          {task && <Card title="升级任务">
            <Descriptions column={1}>
              <Descriptions.Item label="目标版本">{task.version} / {task.build}</Descriptions.Item>
              <Descriptions.Item label="状态"><Tag>{task.status}</Tag></Descriptions.Item>
              <Descriptions.Item label="详情">{task.message}</Descriptions.Item>
              <Descriptions.Item label="任务 ID">{task.id}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{task.updatedAt || '—'}</Descriptions.Item>
            </Descriptions>
            {task.status === 'SUCCESS' && <Button onClick={() => window.location.reload()}>刷新并使用新版本</Button>}
          </Card>}
        </Space>
      </PageContainer>
  );
}
