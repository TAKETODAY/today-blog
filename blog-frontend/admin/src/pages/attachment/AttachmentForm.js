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
import { Button, Form, Input, message, Modal, Popconfirm } from 'antd'
import { getSizeString, isNotEmpty } from "@/utils"
import moment from "moment";
import { useModel } from "@@/plugin-model/useModel";
import { deleteAttach } from "./service";

const FormItem = Form.Item

const formLayout = {
  labelCol: { span: 7 },
  wrapperCol: { span: 13 },
}

const handleDelete = async (attach) => {
  const hide = message.loading(`正在删除: ${ attach.name }`)
  try {
    await deleteAttach(attach)
    hide()
    message.success('更新信息成功', 1)
    return true
  }
  catch (error) {
    hide()
    message.error('更新信息失败请重试！')
    return false
  }
}

const AttachmentForm = props => {
  const { initialState } = useModel('@@initialState');
  const { onSubmit: handleUpdate, onCancel: hideUpdateModal, visible, values, onRefresh } = props
  const { options } = initialState
  const [form] = Form.useForm()

  const update = async () => {
    const fields = await form.validateFields()
    handleUpdate({ ...values, ...fields })
  }

  form.setFieldsValue(values)

  const renderContent = attachment => {
    return (
        <>
          <FormItem name="name" label="附件名称" rules={ [{ required: true, message: '请输入附件名称！' }] }>
            <Input placeholder="请输入"/>
          </FormItem>
          <FormItem label="CDN">
            <Input disabled value={ `${ options['site.cdn'] }${ attachment.uri }` }/>
          </FormItem>
          <FormItem name="url" label="附件路径">
            <Input placeholder="请输入"/>
          </FormItem>
          <FormItem name="fileType" label="附件类型">
            <Input disabled/>
          </FormItem>
          <FormItem label="上传时间">
            <Input disabled value={ moment(attachment.id).format("lll") }/>
          </FormItem>
          <FormItem label="附件大小">
            <Input disabled value={ `${ getSizeString(attachment.size) }` }/>
          </FormItem>
        </>
    )
  }

  const buildUrl = () => {
    const cdn = options['site.cdn']
    const origin = isNotEmpty(cdn) ? cdn : location.origin
    return `${ origin }${ values.uri }`
  }
  const setClipboard = () => {
    navigator.clipboard.writeText(buildUrl()).then(function () {
      message.info("复制成功")
    }, function () {
      message.info("复制失败")
    });
  }

  const renderFooter = () => {
    return (
        <>
          <Button type="dashed" target='_blank' href={ buildUrl() } style={ { marginRight: 10 } }>预览</Button>

          <Button onClick={ hideUpdateModal }>取消</Button>
          <Popconfirm
              title="您确定要删除此附件吗?"
              onConfirm={ async () => {
                await handleDelete(values)
                hideUpdateModal()
                onRefresh && onRefresh()
              } }
              okText="确定"
              cancelText="取消"
          >
            <Button danger>删除</Button>
          </Popconfirm>
          <Button type="dashed" onClick={ setClipboard }>复制地址</Button>
          <Button type="primary" onClick={ update }>完成</Button>
        </>
    )
  }

  return (
      <Modal
          title="附件配置"
          width={ 800 }
          destroyOnClose
          footer={ renderFooter() }
          open={ visible }
          onCancel={ () => hideUpdateModal() }
          bodyStyle={ { padding: '32px 40px 48px' } }
      >
        <Form { ...formLayout } form={ form } initialValues={ { ...values } }>
          { renderContent(values) }
        </Form>
      </Modal>
  )
}

export default AttachmentForm
