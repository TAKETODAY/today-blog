import React from 'react';
import { Alert, Form, Input, InputNumber, Modal } from 'antd';
import { ToolOutlined } from '@ant-design/icons';
import type { MaintenanceController } from './useMaintenance';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  maintenance: MaintenanceController;
}

export default function MaintenanceModal({ maintenance }: Props) {
  const t = useSiteTranslation();
  return <Modal
      title={t('开启维护状态', 'Start maintenance')}
      open={maintenance.visible}
      confirmLoading={maintenance.saving}
      okText={t('开启维护', 'Start maintenance')}
      okButtonProps={{ icon: <ToolOutlined/> }}
      onOk={maintenance.enable}
      onCancel={maintenance.close}
  >
    <Alert type="info" showIcon message={t('开启后新的业务请求将返回 503', 'New site requests will return 503')}
           description={t('API 进程不会退出；完成后可在这里关闭，或等待升级流程自动关闭。', 'The API process keeps running. End maintenance here or let the upgrade finish it automatically.')}
           style={{ marginBottom: 16 }}/>
    <Form form={maintenance.form} layout="vertical">
      <Form.Item name="message" label={t('提示信息', 'Maintenance message')} rules={[{ required: true, message: t('请输入维护提示', 'Enter a maintenance message') }]}>
        <Input.TextArea maxLength={200} showCount rows={2}/>
      </Form.Item>
      <Form.Item name="retryAfter" label={t('建议重试间隔（秒）', 'Retry after (seconds)')} rules={[{ required: true }]}>
        <InputNumber min={1} max={86400} precision={0} style={{ width: '100%' }}/>
      </Form.Item>
    </Form>
  </Modal>;
}
