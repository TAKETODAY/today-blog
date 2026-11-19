import React, { useEffect, useState } from 'react';
import { Form, Input, message, Modal } from 'antd';
import { http } from '@/utils/http';
import type { UpgradePackage } from './types';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  item?: UpgradePackage;
  onClose: () => void;
  onSaved: () => Promise<unknown>;
}

export default function EditPackageModal({ item, onClose, onSaved }: Props) {
  const t = useSiteTranslation();
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
      message.warning(t('请输入升级包名称', 'Enter a package name'));
      return;
    }
    setSaving(true);
    try {
      await http.put(`/api/console/maintenance/upgrade/packages/${item.id}`, {
        displayName: name.trim(), note,
      });
      onClose();
      await onSaved();
      message.success(t('升级包已更新', 'Package updated'));
    }
    finally {
      setSaving(false);
    }
  }

  return <Modal title={t('编辑升级包', 'Edit package')} open={Boolean(item)} confirmLoading={saving} okText={t('保存', 'Save')}
                onOk={save} onCancel={onClose}>
    <Form layout="vertical">
      <Form.Item label={t('展示名称', 'Display name')} required>
        <Input value={name} maxLength={255} onChange={event => setName(event.target.value)}/>
      </Form.Item>
      <Form.Item label={t('备注', 'Notes')}>
        <Input.TextArea value={note} maxLength={500} showCount rows={3}
                        onChange={event => setNote(event.target.value)}/>
      </Form.Item>
    </Form>
  </Modal>;
}
