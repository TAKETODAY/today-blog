import React from 'react';
import { Form, InputNumber, Switch } from 'antd';

export default function OptionFields() {
  return <>
    <Form.Item name="backupEnabled" label="升级前备份数据库" valuePropName="checked"
               extra="备份失败将终止升级；关闭后必须自行保证有可恢复的备份。">
      <Switch checkedChildren="开启" unCheckedChildren="关闭"/>
    </Form.Item>
    <Form.Item name="backupTimeoutSeconds" label="备份超时（秒）" rules={[{ required: true }]}>
      <InputNumber min={30} max={3600} precision={0}/>
    </Form.Item>
    <Form.Item name="healthTimeoutSeconds" label="健康检查超时（秒）" rules={[{ required: true }]}>
      <InputNumber min={10} max={600} precision={0}/>
    </Form.Item>
  </>;
}
