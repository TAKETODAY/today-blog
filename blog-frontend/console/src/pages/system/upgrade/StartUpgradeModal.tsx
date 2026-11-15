import React, { useEffect, useState } from 'react';
import { Alert, Checkbox, Form, Input, Modal, Space } from 'antd';
import { ArrowUpOutlined } from '@ant-design/icons';
import { http } from '@/utils/http';
import type { Environment, Options, UpgradePackage, UpgradeRecord, VersionInfo } from './types';
import { requestOptions } from './utils';
import OptionFields from './OptionFields';

interface Props {
  item?: UpgradePackage;
  version: VersionInfo;
  environment?: Environment;
  loaded: boolean;
  options: Options;
  onClose: () => void;
  onStarted: (record: UpgradeRecord) => void;
}

export default function StartUpgradeModal({
  item, version, environment, loaded, options, onClose, onStarted,
}: Props) {
  const [form] = Form.useForm<Options>();
  const [starting, setStarting] = useState(false);
  const [deletePackageAfter, setDeletePackageAfter] = useState(false);
  const backupEnabled = Form.useWatch('backupEnabled', form);

  useEffect(() => {
    if (!item) {
      return;
    }
    form.setFieldsValue(options);
    setDeletePackageAfter(false);
  }, [item, options, form]);

  const environmentReady = loaded && environment?.updaterReady && environment.installFreeBytes >= 3 * 1024 ** 3
      && (!backupEnabled || (environment.backupReady && environment.backupFreeBytes >= 256 * 1024 ** 2));

  async function start() {
    if (!item) {
      return;
    }
    const values = await form.validateFields();
    setStarting(true);
    try {
      const response = await http.post(`/api/console/maintenance/upgrade/packages/${item.id}/start`, {
        options: requestOptions(values), deletePackage: deletePackageAfter,
      });
      onStarted(response.data);
    }
    finally {
      setStarting(false);
    }
  }

  return <Modal
      title={item ? `升级到 ${item.version} / ${item.build}` : '开始升级'}
      open={Boolean(item)}
      forceRender
      confirmLoading={starting}
      okText="开始升级"
      okButtonProps={{ disabled: !environmentReady, icon: <ArrowUpOutlined/> }}
      onOk={start}
      onCancel={onClose}
  >
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type="info" showIcon
             message={item?.displayName || item?.originalName}
             description={`当前版本 ${version.version} / ${version.build || '—'}；提交后立即进入维护状态。`}/>
      <Form form={form} layout="vertical" disabled={starting || !loaded}>
        <OptionFields/>
        <Form.Item name="note" label="本次升级备注"><Input.TextArea maxLength={500} showCount rows={2}/></Form.Item>
      </Form>
      <Checkbox checked={deletePackageAfter} disabled={starting}
                onChange={event => setDeletePackageAfter(event.target.checked)}>
        升级成功后删除升级包
      </Checkbox>
    </Space>
  </Modal>;
}
