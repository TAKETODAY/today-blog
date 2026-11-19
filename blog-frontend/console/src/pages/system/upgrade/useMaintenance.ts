import { useCallback, useState } from 'react';
import { Form, message, Modal } from 'antd';
import type { FormInstance } from 'antd';
import { http } from '@/utils/http';
import { useSiteTranslation } from '@/utils/use-site-translation';
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
  const t = useSiteTranslation();
  const [status, setStatusState] = useState<MaintenanceStatus>();
  const [saving, setSaving] = useState(false);
  const [visible, setVisible] = useState(false);
  const [form] = Form.useForm<MaintenanceFormValues>();

  const setStatus = useCallback((next: MaintenanceStatus) => {
    setStatusState(next);
  }, []);

  function open() {
    form.setFieldsValue({ message: t('系统维护中，请稍后重试', 'Maintenance in progress. Try again later.'), retryAfter: 120 });
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
      message.success(t('维护状态已开启，业务请求将返回 503', 'Maintenance is active. Site requests will return 503.'));
    }
    finally {
      setSaving(false);
    }
  }

  function disable() {
    Modal.confirm({
      title: t('关闭维护状态？', 'End maintenance?'),
      content: t('关闭后业务请求将立即恢复。若升级仍在进行，请勿关闭。', 'Site requests will resume immediately. Do not end maintenance while an upgrade is running.'),
      okText: t('关闭维护', 'End maintenance'),
      cancelText: t('取消', 'Cancel'),
      async onOk() {
        setSaving(true);
        try {
          const response = await http.put('/api/console/maintenance', { enabled: false });
          setStatusState(response.data);
          message.success(t('维护状态已关闭', 'Maintenance ended'));
        }
        finally {
          setSaving(false);
        }
      },
    });
  }

  const sourceLabel = status?.source === 'manual' ? t('手动开启，升级后不会自动关闭', 'Started manually; will not end automatically')
    : t('升级流程自动开启，完成后自动关闭', 'Started by the upgrade; ends automatically when it finishes');

  return { status, saving, visible, form, sourceLabel, setStatus, open, close, enable, disable };
}
