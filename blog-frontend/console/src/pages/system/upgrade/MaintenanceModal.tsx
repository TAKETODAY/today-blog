import React from 'react';
import { Alert, Form, Input, InputNumber, Modal } from 'antd';
import { ToolOutlined } from '@ant-design/icons';
import type { MaintenanceController } from './useMaintenance';

interface Props {
  maintenance: MaintenanceController;
}

export default function MaintenanceModal({ maintenance }: Props) {
  return <Modal
      title="开启维护状态"
      open={maintenance.visible}
      confirmLoading={maintenance.saving}
      okText="开启维护"
      okButtonProps={{ icon: <ToolOutlined/> }}
      onOk={maintenance.enable}
      onCancel={maintenance.close}
  >
    <Alert type="info" showIcon message="开启后新的业务请求将返回 503"
           description="API 进程不会退出；完成后可在这里关闭，或等待升级流程自动关闭。"
           style={{ marginBottom: 16 }}/>
    <Form form={maintenance.form} layout="vertical">
      <Form.Item name="message" label="提示信息" rules={[{ required: true, message: '请输入维护提示' }]}>
        <Input.TextArea maxLength={200} showCount rows={2}/>
      </Form.Item>
      <Form.Item name="retryAfter" label="建议重试间隔（秒）" rules={[{ required: true }]}>
        <InputNumber min={1} max={86400} precision={0} style={{ width: '100%' }}/>
      </Form.Item>
    </Form>
  </Modal>;
}
