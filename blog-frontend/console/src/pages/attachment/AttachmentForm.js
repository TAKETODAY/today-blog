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
import { siteText } from '@/utils/site-format';

const FormItem = Form.Item

const formLayout = {
  labelCol: { span: 7 },
  wrapperCol: { span: 13 },
}

const handleDelete = async (attach) => {
  const hide = message.loading(siteText(`正在删除: ${attach.name}`, `Deleting: ${attach.name}`))
  try {
    await deleteAttach(attach)
    hide()
    message.success(siteText('附件已删除', 'Attachment deleted'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(siteText('删除附件失败，请重试', 'Could not delete the attachment. Try again.'))
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
          <FormItem name="name" label={t('附件名称', 'Attachment name')} rules={[{ required: true, message: t('请输入附件名称！', 'Enter an attachment name') }]}>
            <Input placeholder={t('请输入附件名称', 'Enter an attachment name')}/>
          </FormItem>
          <FormItem label="CDN">
            <Input disabled value={`${cdn}${attachment.uri}`}/>
          </FormItem>
          <FormItem name="url" label={t('附件路径', 'Attachment path')}>
            <Input placeholder={t('请输入附件路径', 'Enter a path')}/>
          </FormItem>
          <FormItem name="fileType" label={t('附件类型', 'File type')}>
            <Input disabled/>
          </FormItem>
          <FormItem label={t('上传时间', 'Uploaded')}>
            <Input disabled value={format(attachment.createAt)}/>
          </FormItem>
          <FormItem label={t('附件大小', 'File size')}>
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
      message.info(t('复制成功', 'Link copied'))
    }, function () {
      message.info(t('复制失败', 'Could not copy the link'))
    });
  }

  const renderFooter = () => {
    return (
        <>
          <Button type="dashed" target='_blank' href={buildUrl()} style={{ marginRight: 10 }}>{t('预览', 'Preview')}</Button>

          <Button onClick={hideUpdateModal}>{t('取消', 'Cancel')}</Button>
          <Popconfirm
              title={t('您确定要删除此附件吗?', 'Delete this attachment?')}
              onConfirm={async () => {
                await handleDelete(values)
                hideUpdateModal()
                onRefresh && onRefresh()
              }}
              okText={t('确定', 'Confirm')}
              cancelText={t('取消', 'Cancel')}
          >
            <Button danger>{t('删除', 'Delete')}</Button>
          </Popconfirm>
          <Button type="dashed" onClick={setClipboard}>{t('复制地址', 'Copy link')}</Button>
          <Button type="primary" onClick={update}>{t('完成', 'Done')}</Button>
        </>
    )
  }

  return (
      <Modal title={t('附件配置', 'Edit attachment')}
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
