import React from 'react'
import { message, Modal, Upload } from 'antd'
import { InboxOutlined } from '@ant-design/icons';

const { Dragger } = Upload;

const draggerProps = {
  name: 'file',
  multiple: true,
  action: '/api/attachments',
  onChange(info) {
    console.log(info)
    const { status, response } = info.file;
    if (status === 'done') {
      message.success(`'${info.file.name}' 上传成功.`);
    }
    else if (status === 'error') {
      if (response) {
        message.error(`'${info.file.name}' 上传失败: ${response.message}`)
      }
      else {
        message.error(`'${info.file.name}' 上传失败.`)
      }
    }
  },
};

const UploadForm = props => {
  const { onCancel: hideUpdateModal, updateModalVisible, onRefresh } = props

  return (
      <Modal
          title="上传附件"
          width={800}
          destroyOnClose
          onOk={() => {
            hideUpdateModal()
            onRefresh && onRefresh()
          }}
          onCancel={() => {
            hideUpdateModal()
          }}
          visible={updateModalVisible}
          bodyStyle={{ padding: '32px 40px 48px' }}
      >
        <Dragger {...draggerProps}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined/>
          </p>
          <p className="ant-upload-text">单击或拖动文件到该区域以上传</p>
          <p className="ant-upload-hint">支持单次或批量上传</p>
        </Dragger>
      </Modal>
  )
}

export default UploadForm
