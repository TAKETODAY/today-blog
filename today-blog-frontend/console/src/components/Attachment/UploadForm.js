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
import { useSiteTranslation } from '@/utils/use-site-translation';
import { siteText } from '@/utils/site-i18n';

const { Dragger } = Upload;

const draggerProps = {
  name: 'file',
  multiple: true,
  action: '/api/console/attachments',
  headers: { 'X-Blog-Locale': 'site' },
  onChange(info) {
    const { status } = info.file;
    if (status === 'done') {
      message.success(siteText('attachment.uploaded', { name: info.file.name }));
    }
    else if (status === 'error') {
      message.error(siteText('attachment.uploadFailed', { name: info.file.name }));
    }
  },
};

const UploadForm = props => {
  const t = useSiteTranslation();
  const { onCancel: hideUpdateModal, open, onRefresh } = props

  return (
    <Modal
      title={t('attachment.uploadAttachments')}
      width={800}
      destroyOnClose
      onOk={() => {
        hideUpdateModal()
        onRefresh && onRefresh()
      }}
      onCancel={() => {
        hideUpdateModal()
      }}
      open={open}
      bodyStyle={{ padding: '32px 40px 48px' }}
    >
      <Dragger {...draggerProps}>
        <p className="ant-upload-drag-icon">
          <InboxOutlined/>
        </p>
        <p className="ant-upload-text">{t('attachment.clickOrDragFilesHere')}</p>
        <p className="ant-upload-hint">{t('attachment.uploadOneOrMoreFiles')}</p>
      </Dragger>
    </Modal>
  )
}

export default UploadForm
