import React from 'react';
import { Form, InputNumber, Switch } from 'antd';
import { useSiteTranslation } from '@/utils/use-site-translation';

export default function OptionFields() {
  const t = useSiteTranslation();
  return <>
    <Form.Item name="backupEnabled" label={t('upgrade.backUpTheDatabaseBefore')} valuePropName="checked"
               extra={t('upgrade.aBackupFailureAbortsThe')}>
      <Switch checkedChildren={t('upgrade.on')} unCheckedChildren={t('upgrade.off')}/>
    </Form.Item>
    <Form.Item name="backupTimeoutSeconds" label={t('upgrade.backupTimeoutSeconds')} rules={[{ required: true }]}>
      <InputNumber min={30} max={3600} precision={0}/>
    </Form.Item>
    <Form.Item name="healthTimeoutSeconds" label={t('upgrade.healthCheckTimeoutSeconds')} rules={[{ required: true }]}>
      <InputNumber min={10} max={600} precision={0}/>
    </Form.Item>
  </>;
}
