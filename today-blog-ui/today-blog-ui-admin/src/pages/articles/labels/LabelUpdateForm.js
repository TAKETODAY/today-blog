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
  const [tag, setTag] = useState({ ...values })

  const update = async () => {
    const fields = await form.validateFields()
    setTag({ ...tag, ...fields })
    handleUpdate({ ...tag, ...fields })
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
          visible={ updateModalVisible }
          onCancel={ () => hideUpdateModal() }
      >
        <Form { ...formLayout } form={ form } initialValues={ { ...tag } }>
          <FormItem name="name" label="标签名" rules={ [{ required: true, message: '请输入标签名！' }] }>
            <Input placeholder="请输入"/>
          </FormItem>
        </Form>
      </Modal>
  )
}
