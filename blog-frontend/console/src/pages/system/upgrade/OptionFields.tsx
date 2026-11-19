import React from 'react';
import { Form, InputNumber, Switch } from 'antd';
import { useSiteTranslation } from '@/utils/use-site-translation';

export default function OptionFields() {
  const t = useSiteTranslation();
  return <>
    <Form.Item name="backupEnabled" label={t('升级前备份数据库', 'Back up the database before upgrading')} valuePropName="checked"
               extra={t('备份失败将终止升级；关闭后必须自行保证有可恢复的备份。', 'A backup failure aborts the upgrade. If disabled, make sure you have a restorable backup.')}>
      <Switch checkedChildren={t('开启', 'On')} unCheckedChildren={t('关闭', 'Off')}/>
    </Form.Item>
    <Form.Item name="backupTimeoutSeconds" label={t('备份超时（秒）', 'Backup timeout (seconds)')} rules={[{ required: true }]}>
      <InputNumber min={30} max={3600} precision={0}/>
    </Form.Item>
    <Form.Item name="healthTimeoutSeconds" label={t('健康检查超时（秒）', 'Health check timeout (seconds)')} rules={[{ required: true }]}>
      <InputNumber min={10} max={600} precision={0}/>
    </Form.Item>
  </>;
}
