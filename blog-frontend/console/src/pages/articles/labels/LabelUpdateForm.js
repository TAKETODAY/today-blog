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

export default props => {
  const { onSubmit: handleUpdate, onCancel: hideUpdateModal, updateModalVisible, values } = props
  const [form] = Form.useForm()

  const update = async () => {
    const fields = await form.validateFields()
    handleUpdate({ ...values, ...fields })
  }

  form.setFieldsValue(values)

  return (
      <Modal
          title="文章标签更新"
          width={ 800 }
          destroyOnClose
          footer={ <>
            <Button onClick={ hideUpdateModal }>取消</Button>
            <Button type="primary" onClick={ update }>更新</Button>
          </> }
          open={ updateModalVisible }
          onCancel={ () => hideUpdateModal() }
      >
        <Form { ...formLayout } form={ form } initialValues={ { ...values } }>
          <FormItem name="name" label="标签名" rules={ [{ required: true, message: '请输入标签名！' }] }>
            <Input placeholder="请输入"/>
          </FormItem>
        </Form>
      </Modal>
  )
}
