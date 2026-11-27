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
      message.error(t('apiKeys.couldNotCreateTheKey'));
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: number) => {
    setDeleting(id);
    try {
      await deleteApiKey(id);
      message.success(t('apiKeys.apiKeyDeleted'));
      actionRef.current?.reload();
    } catch {
      message.error(t('apiKeys.couldNotDeleteTheKey'));
    } finally {
      setDeleting(null);
    }
  };

  const handleRevoke = async (id: number) => {
    setRevoking(id);
    try {
      await revokeApiKey(id);
      message.success(t('apiKeys.apiKeyRevoked'));
      actionRef.current?.reload();
    } catch {
      message.error(t('apiKeys.couldNotRevokeTheKey'));
    } finally {
      setRevoking(null);
    }
  };

  const columns: ProColumns<ApiKeyItem>[] = [
    { title: t('apiKeys.name'), dataIndex: 'name', ellipsis: true },
    { title: t('apiKeys.status'), width: 100, render: (_, item) => item.revokedAt ? <Tag>{t('apiKeys.revoked')}</Tag> : <Tag color="success">{t('apiKeys.active')}</Tag> },
    { title: t('apiKeys.created'), width: 260, render: (_, item) => format(item.createdAt) },
    { title: t('apiKeys.lastUsed'), width: 260, render: (_, item) => item.lastUsedAt ? format(item.lastUsedAt) : t('apiKeys.neverUsed') },
    { title: t('apiKeys.revokedAt'), width: 260, render: (_, item) => item.revokedAt ? format(item.revokedAt) : 'N/A' },
    {
      title: t('apiKeys.actions'), valueType: 'option', width: 100,
      render: (_, item) => item.revokedAt ? (
        <Popconfirm title={t('apiKeys.confirmDelete', { name: item.name })}
          okText={t('apiKeys.delete')} cancelText={t('apiKeys.cancel')} onConfirm={() => handleDelete(item.id)}
          disabled={deleting !== null}>
          <Button type="link" danger icon={<DeleteOutlined />} loading={deleting === item.id}
            disabled={deleting !== null && deleting !== item.id}>{t('apiKeys.delete')}</Button>
        </Popconfirm>
      ) : (
        <Popconfirm title={t('apiKeys.confirmRevoke', { name: item.name })}
          okText={t('apiKeys.revoke')} cancelText={t('apiKeys.cancel')} onConfirm={() => handleRevoke(item.id)}
          disabled={revoking !== null || deleting !== null}>
          <Button type="link" danger loading={revoking === item.id}
            disabled={revoking !== null && revoking !== item.id || deleting !== null}>{t('apiKeys.revoke')}</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <PageContainer header={{ title: t('apiKeys.apiKeys') }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" showIcon message={t('apiKeys.useAPIKeysWithScripts')}
          description={t('apiKeys.aKeyInheritsTheAccount')} />
        <ProTable<ApiKeyItem> rowKey="id" headerTitle={t('apiKeys.myAPIKeys')} actionRef={actionRef}
          columns={columns} search={false} pagination={false} scroll={{ x: 1000 }}
          request={async () => {
            const data = await listApiKeys();
            return { data, success: true, total: data.length };
          }}
          toolBarRender={() => [
            <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>{t('apiKeys.createAPIKey')}</Button>,
          ]} />
      </Space>

      <Modal title={t('apiKeys.createAPIKey')} visible={createVisible} onOk={handleCreate} confirmLoading={creating}
        okText={t('apiKeys.create')} cancelText={t('apiKeys.cancel')} closable={!creating} maskClosable={!creating}
        cancelButtonProps={{ disabled: creating }} onCancel={() => { if (!creating) setCreateVisible(false); }}>
        <Form form={form} layout="vertical">
          <Form.Item label={t('apiKeys.name')} name="name" extra={t('apiKeys.forExamplePublishingScriptOr')}
            rules={[{ required: true, whitespace: true, message: t('apiKeys.enterAKeyName') }, { max: 100, message: t('apiKeys.useAtMost100Characters') }]}>
            <Input maxLength={100} placeholder={t('apiKeys.nameTheToolThatWill')} disabled={creating} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={t('apiKeys.apiKeyCreated')} visible={created !== null} closable={false} maskClosable={false}
        footer={<Button type="primary" onClick={() => setCreated(null)}>{t('apiKeys.iSavedTheKey')}</Button>}>
        {created && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Alert type="warning" showIcon message={t('apiKeys.copyAndSaveThisKey')}
              description={t('apiKeys.youCannotViewThisKey')} />
            <Typography.Text strong>{created.apiKey.name}</Typography.Text>
            <Typography.Paragraph copyable={{ text: created.value }} style={{ wordBreak: 'break-all' }}>
              <Typography.Text code>{created.value}</Typography.Text>
            </Typography.Paragraph>
            <Typography.Text type="secondary">{t('apiKeys.includeThisRequestHeader')}</Typography.Text>
            <Typography.Text code>Authorization: Bearer &lt;API Key&gt;</Typography.Text>
          </Space>
        )}
      </Modal>
    </PageContainer>
  );
};

export default ApiKeyManagement;
