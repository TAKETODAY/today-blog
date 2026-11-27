import React, { useState } from 'react';
import { Alert, Button, message, Modal, Space, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { http } from '@/utils/http';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  open: boolean;
  onClose: () => void;
  onUploaded: () => Promise<unknown>;
}

export default function UploadPackageModal({ open, onClose, onUploaded }: Props) {
  const t = useSiteTranslation();
  const [file, setFile] = useState<File>();
  const [uploading, setUploading] = useState(false);

  function close() {
    onClose();
    setFile(undefined);
  }

  async function upload() {
    if (!file) {
      message.warning(t('upgrade.chooseAPackage'));
      return;
    }
    setUploading(true);
    try {
      const data = new FormData();
      data.append('file', file);
      await http.post('/api/console/maintenance/upgrade/packages', data, { timeout: 300000 });
      close();
      await onUploaded();
      message.success(t('upgrade.packageIntegrityVerifiedMakeSure'));
    }
    finally {
      setUploading(false);
    }
  }

  return <Modal title={t('upgrade.uploadPackage')} open={open} confirmLoading={uploading} okText={t('upgrade.upload')}
                onOk={upload} onCancel={close}>
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type="info" showIcon message={t('upgrade.fullReleaseZIPOnlyUp')}
             description={t('upgrade.checksumsVerifyIntegrityOnlyUpload')}/>
      <Upload accept=".zip" showUploadList={false} maxCount={1}
              beforeUpload={next => { setFile(next); return false; }}>
        <Button icon={<UploadOutlined/>}>{t('upgrade.chooseZIPFile')}</Button>
      </Upload>
      <span>{file ? file.name : t('upgrade.noFileSelected')}</span>
    </Space>
  </Modal>;
}
