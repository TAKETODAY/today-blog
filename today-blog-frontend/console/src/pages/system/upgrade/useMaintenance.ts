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
    form.setFieldsValue({ message: t('upgrade.maintenanceInProgressTryAgain'), retryAfter: 120 });
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
      message.success(t('upgrade.maintenanceIsActiveSiteRequests'));
    }
    finally {
      setSaving(false);
    }
  }

  function disable() {
    Modal.confirm({
      title: t('upgrade.confirmEndMaintenance'),
      content: t('upgrade.siteRequestsWillResumeImmediately'),
      okText: t('upgrade.endMaintenance'),
      cancelText: t('upgrade.cancel'),
      async onOk() {
        setSaving(true);
        try {
          const response = await http.put('/api/console/maintenance', { enabled: false });
          setStatusState(response.data);
          message.success(t('upgrade.maintenanceEnded'));
        }
        finally {
          setSaving(false);
        }
      },
    });
  }

  const sourceLabel = status?.source === 'manual' ? t('upgrade.startedManuallyWillNotEnd')
    : t('upgrade.startedByTheUpgradeEnds');

  return { status, saving, visible, form, sourceLabel, setStatus, open, close, enable, disable };
}
