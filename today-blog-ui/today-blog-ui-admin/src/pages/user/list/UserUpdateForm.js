import React, { useState } from 'react'
import { Button, Form, Input, Modal } from 'antd'

const { TextArea } = Input
const FormItem = Form.Item

const formLayout = {
  labelCol: { span: 7 },
  wrapperCol: { span: 13 },
}

const UserUpdateForm = props => {
  const { onSubmit: handleUpdate, onCancel: hideUpdateModal, updateModalVisible, values } = props
  const [form] = Form.useForm()
  const [user, setUser] = useState({ ...values })

  const update = async () => {
    const fields = await form.validateFields()
    const user =  { ...values, ...fields }
    setUser(user)
    handleUpdate(user)
  }

  const mergeUser = (newUser) => {
    setUser({ ...user, ...newUser })
  }

  form.setFieldsValue(values)

  const renderFooter = () => {
    return (
        <>
          <Button onClick={ hideUpdateModal }>取消</Button>
          <Button type="primary" onClick={ update }>更新</Button>
        </>
    )
  }
  return (
      <Modal
          title="用户信息设置"
          width={ 800 }
          destroyOnClose={ true }
          footer={ renderFooter() }
          visible={ updateModalVisible }
          onCancel={ () => hideUpdateModal() }
          bodyStyle={ { padding: '32px 40px 48px' } }
      >
        <Form { ...formLayout } form={ form }>
          <FormItem name="name" label="昵称" rules={ [{ required: true, message: '请输入书名！' }] }>
            <Input placeholder="请输入"/>
          </FormItem>
          <FormItem name="introduce" label="介绍" rules={ [{ required: true, message: '请输入作者名！' }] }>
            <TextArea placeholder="请输入" rows={ 3 }/>
          </FormItem>
          <FormItem name="site" label="个人网站" rules={ [{ required: true, message: '请输入出版社！' }] }>
            <Input placeholder="请输入"/>
          </FormItem>
        </Form>
      </Modal>
  )
}

export default UserUpdateForm
