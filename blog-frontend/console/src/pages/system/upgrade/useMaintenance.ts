import { useCallback, useState } from 'react';
import { Form, message, Modal } from 'antd';
import type { FormInstance } from 'antd';
import { http } from '@/utils/http';
import type { MaintenanceStatus } from './types';

interface MaintenanceFormValues {
  message: string;
  retryAfter: number;
}

export interface MaintenanceController {
  status?: MaintenanceStatus;
  saving: boolean;
  visible: boolean;
  form: FormInstance<MaintenanceFormValues>;
  sourceLabel: string;
  setStatus: (status: MaintenanceStatus) => void;
  open: () => void;
  close: () => void;
  enable: () => Promise<void>;
  disable: () => void;
}

export function useMaintenance(): MaintenanceController {
  const [status, setStatusState] = useState<MaintenanceStatus>();
  const [saving, setSaving] = useState(false);
  const [visible, setVisible] = useState(false);
  const [form] = Form.useForm<MaintenanceFormValues>();

  const setStatus = useCallback((next: MaintenanceStatus) => {
    setStatusState(next);
  }, []);

  function open() {
    form.setFieldsValue({ message: '系统维护中，请稍后重试', retryAfter: 120 });
    setVisible(true);
  }

  function close() {
    setVisible(false);
  }

  async function enable() {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const response = await http.put('/api/console/maintenance', {
        enabled: true, message: values.message, retryAfter: values.retryAfter,
      });
      setStatusState(response.data);
      setVisible(false);
      message.success('维护状态已开启，业务请求将返回 503');
    }
    finally {
      setSaving(false);
    }
  }

  function disable() {
    Modal.confirm({
      title: '关闭维护状态？',
      content: '关闭后业务请求将立即恢复。若升级仍在进行，请勿关闭。',
      okText: '关闭维护',
      async onOk() {
        setSaving(true);
        try {
          const response = await http.put('/api/console/maintenance', { enabled: false });
          setStatusState(response.data);
          message.success('维护状态已关闭');
        }
        finally {
          setSaving(false);
        }
      },
    });
  }

  const sourceLabel = status?.source === 'manual' ? '手动开启，升级后不会自动关闭' : '升级流程自动开启，完成后自动关闭';

  return { status, saving, visible, form, sourceLabel, setStatus, open, close, enable, disable };
}
