/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import React from 'react'
import { message, Modal, Upload } from 'antd'
import { InboxOutlined } from '@ant-design/icons';

const { Dragger } = Upload;

const draggerProps = {
  name: 'file',
  multiple: true,
  action: '/api/attachments',
  onChange(info) {
    const { status } = info.file;
    if (status === 'done') {
      message.success(`'${info.file.name}' 上传成功.`);
    }
    else if (status === 'error') {
      message.error(`'${info.file.name}' 上传失败.`);
    }
  },
};

const UploadForm = props => {
  const { onCancel: hideUpdateModal, visible, onRefresh } = props

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
      open={visible}
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
