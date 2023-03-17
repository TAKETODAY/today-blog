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

import { useEffect, useState } from 'react';
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
