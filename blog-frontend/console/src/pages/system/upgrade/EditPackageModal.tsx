import React, { useEffect, useState } from 'react';
import { Form, Input, message, Modal } from 'antd';
import { http } from '@/utils/http';
import type { UpgradePackage } from './types';

interface Props {
  item?: UpgradePackage;
  onClose: () => void;
  onSaved: () => Promise<unknown>;
}

export default function EditPackageModal({ item, onClose, onSaved }: Props) {
  const [name, setName] = useState('');
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!item) {
      return;
    }
    setName(item.displayName || item.originalName);
    setNote(item.note || '');
  }, [item]);

  async function save() {
    if (!item) {
      return;
    }
    if (!name.trim()) {
      message.warning('请输入升级包名称');
      return;
    }
    setSaving(true);
    try {
      await http.put(`/api/console/maintenance/upgrade/packages/${item.id}`, {
        displayName: name.trim(), note,
      });
      onClose();
      await onSaved();
      message.success('升级包已更新');
    }
    finally {
      setSaving(false);
    }
  }

  return <Modal title="编辑升级包" open={Boolean(item)} confirmLoading={saving} okText="保存"
                onOk={save} onCancel={onClose}>
    <Form layout="vertical">
      <Form.Item label="展示名称" required>
        <Input value={name} maxLength={255} onChange={event => setName(event.target.value)}/>
      </Form.Item>
      <Form.Item label="备注">
        <Input.TextArea value={note} maxLength={500} showCount rows={3}
                        onChange={event => setNote(event.target.value)}/>
      </Form.Item>
    </Form>
  </Modal>;
}
