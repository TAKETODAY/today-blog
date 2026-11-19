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
      message.warning(t('请选择升级包', 'Choose a package'));
      return;
    }
    setUploading(true);
    try {
      const data = new FormData();
      data.append('file', file);
      await http.post('/api/console/maintenance/upgrade/packages', data, { timeout: 300000 });
      close();
      await onUploaded();
      message.success(t('升级包完整性校验通过，请确认来源可信', 'Package integrity verified. Make sure you trust its source.'));
    }
    finally {
      setUploading(false);
    }
  }

  return <Modal title={t('上传升级包', 'Upload package')} open={open} confirmLoading={uploading} okText={t('上传', 'Upload')}
                onOk={upload} onCancel={close}>
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type="info" showIcon message={t('仅支持完整发行 ZIP，最大 512 MiB', 'Full release ZIP only, up to 512 MiB')}
             description={t('摘要只校验完整性，请仅上传自己构建并通过可信渠道传输的升级包。', 'Checksums verify integrity only. Upload only packages you built and transferred through a trusted channel.')}/>
      <Upload accept=".zip" showUploadList={false} maxCount={1}
              beforeUpload={next => { setFile(next); return false; }}>
        <Button icon={<UploadOutlined/>}>{t('选择 ZIP 文件', 'Choose ZIP file')}</Button>
      </Upload>
      <span>{file ? file.name : t('尚未选择文件', 'No file selected')}</span>
    </Space>
  </Modal>;
}
