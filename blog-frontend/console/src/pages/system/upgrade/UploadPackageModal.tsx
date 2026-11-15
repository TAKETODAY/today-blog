import React, { useState } from 'react';
import { Alert, Button, message, Modal, Space, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { http } from '@/utils/http';

interface Props {
  open: boolean;
  onClose: () => void;
  onUploaded: () => Promise<unknown>;
}

export default function UploadPackageModal({ open, onClose, onUploaded }: Props) {
  const [file, setFile] = useState<File>();
  const [uploading, setUploading] = useState(false);

  function close() {
    onClose();
    setFile(undefined);
  }

  async function upload() {
    if (!file) {
      message.warning('请选择升级包');
      return;
    }
    setUploading(true);
    try {
      const data = new FormData();
      data.append('file', file);
      await http.post('/api/console/maintenance/upgrade/packages', data, { timeout: 300000 });
      close();
      await onUploaded();
      message.success('升级包完整性校验通过，请确认来源可信');
    }
    finally {
      setUploading(false);
    }
  }

  return <Modal title="上传升级包" open={open} confirmLoading={uploading} okText="上传"
                onOk={upload} onCancel={close}>
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type="info" showIcon message="仅支持完整发行 ZIP，最大 512 MiB"
             description="摘要只校验完整性，请仅上传自己构建并通过可信渠道传输的升级包。"/>
      <Upload accept=".zip" showUploadList={false} maxCount={1}
              beforeUpload={next => { setFile(next); return false; }}>
        <Button icon={<UploadOutlined/>}>选择 ZIP 文件</Button>
      </Upload>
      <span>{file ? file.name : '尚未选择文件'}</span>
    </Space>
  </Modal>;
}
