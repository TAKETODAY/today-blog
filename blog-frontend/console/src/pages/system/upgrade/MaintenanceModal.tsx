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
      title={t('upgrade.startMaintenanceTitle')}
      open={maintenance.visible}
      confirmLoading={maintenance.saving}
      okText={t('upgrade.startMaintenance')}
      okButtonProps={{ icon: <ToolOutlined/> }}
      onOk={maintenance.enable}
      onCancel={maintenance.close}
  >
    <Alert type="info" showIcon message={t('upgrade.newSiteRequestsWillReturn')}
           description={t('upgrade.theAPIProcessKeepsRunning')}
           style={{ marginBottom: 16 }}/>
    <Form form={maintenance.form} layout="vertical">
      <Form.Item name="message" label={t('upgrade.maintenanceMessage')} rules={[{ required: true, message: t('upgrade.enterAMaintenanceMessage') }]}>
        <Input.TextArea maxLength={200} showCount rows={2}/>
      </Form.Item>
      <Form.Item name="retryAfter" label={t('upgrade.retryAfterSeconds')} rules={[{ required: true }]}>
        <InputNumber min={1} max={86400} precision={0} style={{ width: '100%' }}/>
      </Form.Item>
    </Form>
  </Modal>;
}
