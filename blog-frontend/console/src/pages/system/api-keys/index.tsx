import React, { useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Alert, Button, Form, Input, message, Modal, Popconfirm, Space, Tag, Typography } from 'antd';
import { format } from '@/utils';
import { useSiteTranslation } from '@/utils/use-site-translation';
import { ApiKeyItem, CreatedApiKey, createApiKey, deleteApiKey, listApiKeys, revokeApiKey } from './service';

const ApiKeyManagement: React.FC = () => {
  const t = useSiteTranslation();
  const actionRef = useRef<ActionType>();
  const [form] = Form.useForm<{ name: string }>();
  const [createVisible, setCreateVisible] = useState(false);
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState<CreatedApiKey | null>(null);
  const [revoking, setRevoking] = useState<number | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);

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
      message.error(t('创建失败，请重试', 'Could not create the key. Try again.'));
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number) => {
    setDeleting(id);
    try {
      await deleteApiKey(id);
      message.success(t('API Key 已删除', 'API key deleted'));
      actionRef.current?.reload();
    } catch {
      message.error(t('删除失败，请重试', 'Could not delete the key. Try again.'));
    } finally {
      setDeleting(null);
    }
  };

  const handleRevoke = async (id: number) => {
    setRevoking(id);
    try {
      await revokeApiKey(id);
      message.success(t('API Key 已撤销', 'API key revoked'));
      actionRef.current?.reload();
    } catch {
      message.error(t('撤销失败，请重试', 'Could not revoke the key. Try again.'));
    } finally {
      setRevoking(null);
    }
  };

  const columns: ProColumns<ApiKeyItem>[] = [
    { title: t('名称', 'Name'), dataIndex: 'name', ellipsis: true },
    { title: t('状态', 'Status'), width: 100, render: (_, item) => item.revokedAt ? <Tag>{t('已撤销', 'Revoked')}</Tag> : <Tag color="success">{t('有效', 'Active')}</Tag> },
    { title: t('创建时间', 'Created'), width: 260, render: (_, item) => format(item.createdAt) },
    { title: t('最近使用', 'Last used'), width: 260, render: (_, item) => item.lastUsedAt ? format(item.lastUsedAt) : t('尚未使用', 'Never used') },
    { title: t('撤销时间', 'Revoked'), width: 260, render: (_, item) => item.revokedAt ? format(item.revokedAt) : 'N/A' },
    {
      title: t('操作', 'Actions'), valueType: 'option', width: 100,
      render: (_, item) => item.revokedAt ? (
        <Popconfirm title={t(`删除「${item.name}」？删除后无法恢复。`, `Delete “${item.name}”? This cannot be undone.`)}
          okText={t('删除', 'Delete')} cancelText={t('取消', 'Cancel')} onConfirm={() => handleDelete(item.id)}
          disabled={deleting !== null}>
          <Button type="link" danger icon={<DeleteOutlined />} loading={deleting === item.id}
            disabled={deleting !== null && deleting !== item.id}>{t('删除', 'Delete')}</Button>
        </Popconfirm>
      ) : (
        <Popconfirm title={t(`撤销「${item.name}」？使用此密钥的接口调用将失效。`, `Revoke “${item.name}”? API calls using it will stop working.`)}
          okText={t('撤销', 'Revoke')} cancelText={t('取消', 'Cancel')} onConfirm={() => handleRevoke(item.id)}
          disabled={revoking !== null || deleting !== null}>
          <Button type="link" danger loading={revoking === item.id}
            disabled={revoking !== null && revoking !== item.id || deleting !== null}>{t('撤销', 'Revoke')}</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <PageContainer header={{ title: t('API Key 管理', 'API keys') }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" showIcon message={t('用于脚本和工具调用博客接口', 'Use API keys with scripts and tools')}
          description={t('密钥具有所属账号的权限；博主密钥具有完整管理权限。密钥原文仅在创建时显示，请为不同工具分别创建密钥。',
            'A key inherits the account’s permissions. Blogger keys have full administrative access. Keys are shown only when created; use a separate key for each tool.')} />
        <ProTable<ApiKeyItem> rowKey="id" headerTitle={t('我的 API Key', 'My API keys')} actionRef={actionRef}
          columns={columns} search={false} pagination={false} scroll={{ x: 1000 }}
          request={async () => {
            const data = await listApiKeys();
            return { data, success: true, total: data.length };
          }}
          toolBarRender={() => [
            <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>{t('创建 API Key', 'Create API key')}</Button>,
          ]} />
      </Space>

      <Modal title={t('创建 API Key', 'Create API key')} visible={createVisible} onOk={handleCreate} confirmLoading={creating}
        okText={t('创建', 'Create')} cancelText={t('取消', 'Cancel')} closable={!creating} maskClosable={!creating}
        cancelButtonProps={{ disabled: creating }} onCancel={() => { if (!creating) setCreateVisible(false); }}>
        <Form form={form} layout="vertical">
          <Form.Item label={t('名称', 'Name')} name="name" extra={t('例如：本地发布脚本、文章备份工具', 'For example: publishing script or article backup tool')}
            rules={[{ required: true, whitespace: true, message: t('请输入密钥名称', 'Enter a key name') }, { max: 100, message: t('名称最多 100 个字符', 'Use at most 100 characters') }]}>
            <Input maxLength={100} placeholder={t('为使用此密钥的工具起一个名字', 'Name the tool that will use this key')} disabled={creating} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={t('API Key 已创建', 'API key created')} visible={created !== null} closable={false} maskClosable={false}
        footer={<Button type="primary" onClick={() => setCreated(null)}>{t('我已保存，关闭', 'I saved the key')}</Button>}>
        {created && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Alert type="warning" showIcon message={t('请立即复制并保存', 'Copy and save this key now')}
              description={t('关闭后无法再次查看密钥原文。遗失后请撤销并创建新的密钥。', 'You cannot view this key again. If lost, revoke it and create a replacement.')} />
            <Typography.Text strong>{created.apiKey.name}</Typography.Text>
            <Typography.Paragraph copyable={{ text: created.value }} style={{ wordBreak: 'break-all' }}>
              <Typography.Text code>{created.value}</Typography.Text>
            </Typography.Paragraph>
            <Typography.Text type="secondary">{t('调用时添加请求头：', 'Include this request header:')}</Typography.Text>
            <Typography.Text code>Authorization: Bearer &lt;API Key&gt;</Typography.Text>
          </Space>
        )}
      </Modal>
    </PageContainer>
  );
};

export default ApiKeyManagement;
