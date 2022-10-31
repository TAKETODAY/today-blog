import React, { useState } from 'react'
import { Button, Form, Input, Modal } from 'antd'

const FormItem = Form.Item

const formLayout = {
  labelCol: { span: 7 },
  wrapperCol: { span: 13 },
}

const UpdateForm = props => {
  const { onSubmit: handleUpdate, onCancel: hideUpdateModal, updateModalVisible, values } = props
  const [form] = Form.useForm()
  const [comment, setComment] = useState({ ...values })
  const id = values.id
  const update = async () => {
    const fields = await form.validateFields()
    setComment({ ...comment, ...fields })
    handleUpdate(id, { ...comment, ...fields })
  }

  form.setFieldsValue(values)

  const renderContent = () => {
    return (
        <>
          <FormItem name="content" label="评论内容">
            <Input placeholder="请输入"/>
          </FormItem>
          <FormItem name="articleId" label="文章">
            <Input placeholder="请输入"/>
          </FormItem>
          <FormItem name="status" label="状态">
            <Input placeholder="请输入"/>
          </FormItem>
        </>
    )
  }

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
          title="文章分类配置"
          width={ 800 }
          destroyOnClose
          footer={ renderFooter() }
          visible={ updateModalVisible }
          onCancel={ () => hideUpdateModal() }
          bodyStyle={ { padding: '32px 40px 48px' } }
      >
        <Form { ...formLayout } form={ form } initialValues={ { ...comment } }>
          { renderContent() }
        </Form>
      </Modal>
  )
}

export default UpdateForm
