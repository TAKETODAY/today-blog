import React, { useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { PlusOutlined } from '@ant-design/icons';
import { Alert, Button, Form, Input, message, Modal, Popconfirm, Space, Tag, Typography } from 'antd';
import { format } from '@/utils';
import { ApiKeyItem, CreatedApiKey, createApiKey, listApiKeys, revokeApiKey } from './service';

const ApiKeyManagement: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [form] = Form.useForm<{ name: string }>();
  const [createVisible, setCreateVisible] = useState(false);
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState<CreatedApiKey | null>(null);
  const [revoking, setRevoking] = useState<number | null>(null);

  const handleCreate = async () => {
    const values = await form.validateFields().catch(() => null);
    if (!values) return;
    setCreating(true);
    try {
      const result = await createApiKey(values.name.trim());
      setCreated(result);
      setCreateVisible(false);
      form.resetFields();
      actionRef.current?.reload();
    } catch {
      message.error('创建失败，请重试');
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (id: number) => {
    setRevoking(id);
    try {
      await revokeApiKey(id);
      message.success('API Key 已撤销');
      actionRef.current?.reload();
    } catch {
      message.error('撤销失败，请重试');
    } finally {
      setRevoking(null);
    }
  };

  const columns: ProColumns<ApiKeyItem>[] = [
    { title: '名称', dataIndex: 'name', ellipsis: true },
    { title: '状态', width: 100, render: (_, item) => item.revokedAt ? <Tag>已撤销</Tag> : <Tag color="success">有效</Tag> },
    { title: '创建时间', width: 180, render: (_, item) => format(item.createdAt) },
    { title: '最近使用', width: 180, render: (_, item) => item.lastUsedAt ? format(item.lastUsedAt) : '尚未使用' },
    { title: '撤销时间', width: 180, render: (_, item) => item.revokedAt ? format(item.revokedAt) : '—' },
    {
      title: '操作', valueType: 'option', width: 100,
      render: (_, item) => item.revokedAt ? '—' : (
        <Popconfirm title={`撤销「${item.name}」？使用此密钥的接口调用将失效。`}
          okText="撤销" cancelText="取消" onConfirm={() => handleRevoke(item.id)}
          disabled={revoking !== null}>
          <Button type="link" danger loading={revoking === item.id} disabled={revoking !== null && revoking !== item.id}>撤销</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <PageContainer header={{ title: 'API Key 管理' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" showIcon message="用于脚本和工具调用博客接口"
          description="密钥具有所属账号的权限；博主密钥具有完整管理权限。密钥原文仅在创建时显示，请为不同工具分别创建密钥。" />
        <ProTable<ApiKeyItem> rowKey="id" headerTitle="我的 API Key" actionRef={actionRef}
          columns={columns} search={false} pagination={false} scroll={{ x: 1000 }}
          request={async () => {
            const data = await listApiKeys();
            return { data, success: true, total: data.length };
          }}
          toolBarRender={() => [
            <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>创建 API Key</Button>,
          ]} />
      </Space>

      <Modal title="创建 API Key" visible={createVisible} onOk={handleCreate} confirmLoading={creating}
        okText="创建" cancelText="取消" closable={!creating} maskClosable={!creating}
        cancelButtonProps={{ disabled: creating }} onCancel={() => { if (!creating) setCreateVisible(false); }}>
        <Form form={form} layout="vertical">
          <Form.Item label="名称" name="name" extra="例如：本地发布脚本、文章备份工具"
            rules={[{ required: true, whitespace: true, message: '请输入密钥名称' }, { max: 100, message: '名称最多 100 个字符' }]}>
            <Input maxLength={100} placeholder="为使用此密钥的工具起一个名字" disabled={creating} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="API Key 已创建" visible={created !== null} closable={false} maskClosable={false}
        footer={<Button type="primary" onClick={() => setCreated(null)}>我已保存，关闭</Button>}>
        {created && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Alert type="warning" showIcon message="请立即复制并保存"
              description="关闭后无法再次查看密钥原文。遗失后请撤销并创建新的密钥。" />
            <Typography.Text strong>{created.apiKey.name}</Typography.Text>
            <Typography.Paragraph copyable={{ text: created.value }} style={{ wordBreak: 'break-all' }}>
              <Typography.Text code>{created.value}</Typography.Text>
            </Typography.Paragraph>
            <Typography.Text type="secondary">调用时添加请求头：</Typography.Text>
            <Typography.Text code>Authorization: Bearer &lt;API Key&gt;</Typography.Text>
          </Space>
        )}
      </Modal>
    </PageContainer>
  );
};

export default ApiKeyManagement;
