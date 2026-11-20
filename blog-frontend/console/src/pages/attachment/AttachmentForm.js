/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import React from 'react'
import { Button, Form, Input, message, Modal, Popconfirm } from 'antd'
import { format, getSizeString, isNotEmpty } from "@/utils"
import { deleteAttach } from "./service";
import { useCDN } from "@/components/hooks";
import { useSiteTranslation } from '@/utils/use-site-translation';
import { siteText } from '@/utils/site-i18n';

const FormItem = Form.Item

const formLayout = {
  labelCol: { span: 7 },
  wrapperCol: { span: 13 },
}

const handleDelete = async (attach) => {
  const hide = message.loading(siteText('attachment.deleting', { name: attach.name }))
  try {
    await deleteAttach(attach)
    hide()
    message.success(siteText('attachment.attachmentDeleted'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(siteText('attachment.couldNotDeleteTheAttachment'))
    return false
  }
}

const AttachmentForm = props => {
  const t = useSiteTranslation();
  const cdn = useCDN()
  const { onSubmit: handleUpdate, onCancel: hideUpdateModal, visible, values, onRefresh } = props
  const [form] = Form.useForm()

  const update = async () => {
    const fields = await form.validateFields()
    handleUpdate({ ...values, ...fields })
  }

  form.setFieldsValue(values)

  const renderContent = attachment => {
    return (
        <>
          <FormItem name="name" label={t('attachment.attachmentName')} rules={[{ required: true, message: t('attachment.attachmentNameRequired') }]}>
            <Input placeholder={t('attachment.enterAnAttachmentName')}/>
          </FormItem>
          <FormItem label="CDN">
            <Input disabled value={`${cdn}${attachment.uri}`}/>
          </FormItem>
          <FormItem name="url" label={t('attachment.attachmentPath')}>
            <Input placeholder={t('attachment.enterAPath')}/>
          </FormItem>
          <FormItem name="fileType" label={t('attachment.fileType')}>
            <Input disabled/>
          </FormItem>
          <FormItem label={t('attachment.uploadedAt')}>
            <Input disabled value={format(attachment.createAt)}/>
          </FormItem>
          <FormItem label={t('attachment.fileSize')}>
            <Input disabled value={`${getSizeString(attachment.size)}`}/>
          </FormItem>
        </>
    )
  }

  const buildUrl = () => {
    const origin = isNotEmpty(cdn) ? cdn : location.origin
    return `${origin}${values.uri}`
  }

  const setClipboard = () => {
    navigator.clipboard.writeText(buildUrl()).then(function () {
      message.info(t('attachment.linkCopied'))
    }, function () {
      message.info(t('attachment.couldNotCopyTheLink'))
    });
  }

  const renderFooter = () => {
    return (
        <>
          <Button type="dashed" target='_blank' href={buildUrl()} style={{ marginRight: 10 }}>{t('attachment.preview')}</Button>

          <Button onClick={hideUpdateModal}>{t('attachment.cancel')}</Button>
          <Popconfirm
              title={t('attachment.deleteThisAttachment')}
              onConfirm={async () => {
                await handleDelete(values)
                hideUpdateModal()
                onRefresh && onRefresh()
              }}
              okText={t('attachment.confirm')}
              cancelText={t('attachment.cancel')}
          >
            <Button danger>{t('attachment.delete')}</Button>
          </Popconfirm>
          <Button type="dashed" onClick={setClipboard}>{t('attachment.copyLink')}</Button>
          <Button type="primary" onClick={update}>{t('attachment.done')}</Button>
        </>
    )
  }

  return (
      <Modal title={t('attachment.editAttachment')}
             width={800}
             destroyOnClose
             footer={renderFooter()}
             open={visible}
             onCancel={() => hideUpdateModal()}
             bodyStyle={{ padding: '32px 40px 48px' }}
      >
        <Form {...formLayout} form={form} initialValues={{ ...values }}>
          {renderContent(values)}
        </Form>
      </Modal>
  )
}

export default AttachmentForm
