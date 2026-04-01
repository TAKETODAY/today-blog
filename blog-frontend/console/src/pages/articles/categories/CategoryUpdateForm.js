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

import React, { useState } from 'react'
import { Button, Form, Input, InputNumber, Modal } from 'antd'

const FormItem = Form.Item

const formLayout = {
  labelCol: { span: 7 },
  wrapperCol: { span: 13 },
}

const ArticleUpdateForm = props => {
  const { onSubmit: handleUpdate, onCancel: hideUpdateModal, updateModalVisible, values } = props
  const [form] = Form.useForm()
  const [category, setCategory] = useState({ ...values })
  const oldName = values.name
  const update = async () => {
    const fields = await form.validateFields()
    setCategory({ ...category, ...fields })
    handleUpdate(oldName, { ...category, ...fields })
  }

  form.setFieldsValue(values)

  const renderContent = () => {
    return (
        <>
          <FormItem name="order" label="排序" rules={[{ required: true, message: '请输入排序！' }]}>
            <InputNumber min={0} max={128} placeholder="请输入"/>
          </FormItem>
          <FormItem name="name" label="分类名" rules={[{ required: true, message: '请输入分类名！' }]}>
            <Input placeholder="请输入"/>
          </FormItem>
          <FormItem name="description" label="描述" rules={[{ required: true, message: '请输入出描述！' }]}>
            <Input placeholder="请输入"/>
          </FormItem>
        </>
    )
  }

  const renderFooter = () => {
    return (
        <>
          <Button onClick={hideUpdateModal}>取消</Button>
          <Button type="primary" onClick={update}>更新</Button>
        </>
    )
  }

  return (
      <Modal
          title="文章分类配置"
          width={800}
          destroyOnClose
          footer={renderFooter()}
          open={updateModalVisible}
          onCancel={() => hideUpdateModal()}
          bodyStyle={{ padding: '32px 40px 48px' }}
      >
        <Form {...formLayout} form={form} initialValues={{ ...category }}>
          {renderContent()}
        </Form>
      </Modal>
  )
}

export default ArticleUpdateForm
