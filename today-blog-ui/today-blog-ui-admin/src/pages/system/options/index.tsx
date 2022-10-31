import React, { useEffect, useState } from 'react';
import { Button, Form, Input, message } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import { doUpdateOptions, queryOptions } from "./service";


const layout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 16 },
};

export default () => {
  const [options, setOptions] = useState({})
  const [form] = Form.useForm();

  useEffect(() => {
    queryOptions().then(res => {
      setOptions(res.data)
    })
  }, [])

  async function updateOptions() {
    const fieldsValue = form.getFieldsValue()
    const formValues = {}

    Object.entries<string>(fieldsValue).forEach((entry) => {
      const [key, value] = entry
      const option = options[key];
      if (option !== value) {
        formValues[key] = value
      }
    })
    if (Object.keys(formValues).length > 0) {
      const hide = message.loading(`正在删除更新`)
      try {
        await doUpdateOptions(formValues)
        message.success('更新成功，即将刷新', 1)
        queryOptions().then(res => {
          setOptions(res.data)
        })
      }
      catch (error) {
        message.error('更新失败，请重试')
      }
      finally {
        hide()
      }
    }
  }

  return (
      <PageContainer>
        <div style={{ background: '#fff', padding: 10, borderRadius: 5 }}>
          <Form {...layout} form={form} name="options-form">
            {Object.keys(options).map((option) => {
              return (
                  <Form.Item key={option} name={option} label={option} initialValue={options[option]}>
                    <Input/>
                  </Form.Item>
              )
            })}
            <Form.Item wrapperCol={{ span: 14, offset: 4 }}>
              <Button type="primary" onClick={updateOptions}>更新</Button>
            </Form.Item>
          </Form>
        </div>
      </PageContainer>
  )
}
