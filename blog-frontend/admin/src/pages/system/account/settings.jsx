import { Alert, Button, Form, Input, message, Spin, Tabs } from 'antd';
import React, { useState } from 'react';
import ImageCropper from './cropper/ImageCropper';
import UserService from "./service"
import { useModel } from "@@/plugin-model/useModel"
import { extractData, handleValidationError, showHttpErrorMessageVoid } from "@/utils"

const FormItem = Form.Item
const { TabPane } = Tabs;

const userService = new UserService()

const emailRules = [
  { required: true, message: '请输入新的邮箱!' },
  { type: 'email', message: '请您输入正确格式的邮箱!' }
]

const mobilePhoneRules = [
  { required: true, message: '请输入新的手机号!' },
  {
    message: '请您输入正确格式的手机号!',
    pattern: /^1[3456789]\d{9}$/
  }
]

const passwordRules = [
  { required: true, message: '请输入密码!', max: 48 },
]

const newPasswordRules = [
  { required: true, message: '请输入密码!', min: 2, max: 48 },
]

const nameRules = [
  { required: true, message: '请输入姓名或昵称!' }
]

const InfoSettings = ({ updating, userSession, setUpdating, setUserSession }) => {
  const update = data => {
    if (updating) {
      return
    }
    setUpdating(true)
    userService.updateInfo(data).then(extractData).then(user => {
      setUserSession(user)
      message.success("修改成功")
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }

  return (<>
    <Form name="infoForm" layout="vertical" initialValues={ { ...userSession } } onFinish={ update }>
      <FormItem name="name" label="姓名或昵称" rules={ nameRules }>
        <Input maxLength="24" placeholder="请输入姓名或昵称" />
      </FormItem>
      <FormItem name="introduce" label="自我描述">
        <Input maxLength="64" placeholder="请输入自我描述" />
      </FormItem>
      <Button loading={ updating } type="primary" htmlType="submit">保存修改</Button>
    </Form>
  </>)
}

const EmailSettings = ({ updating, userSession, setUpdating, setUserSession }) => {
  const update = data => {
    if (updating) {
      return
    }
    setUpdating(true)
    userService.updateEmailAndMobile(data).then(extractData).then(user => {
      setUserSession(user)
      message.success("修改成功")
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }
  return (
    <>
      <Form name="emailForm" layout="vertical" initialValues={ { ...userSession } } onFinish={ update }>
        <FormItem name="password" label="当前账号密码" rules={ passwordRules }>
          <Input.Password maxLength="24" placeholder="请输入当前账号密码" />
        </FormItem>

        <FormItem name="mobilePhone" label="新手机号" rules={ mobilePhoneRules }>
          <Input maxLength="11" placeholder="请输入新手机号" />
        </FormItem>

        <FormItem name="email" label="新邮箱" rules={ emailRules }>
          <Input maxLength="48" placeholder="请输入新邮箱" />
        </FormItem>
        <Button loading={ updating } type="primary" htmlType="submit" title="保存修改">保存修改</Button>
      </Form>
    </>
  )
}

const PasswordSettings = ({ updating, setUpdating }) => {
  const update = data => {
    const { oldPassword, newPassword, confirmNewPassword } = data
    if (oldPassword === newPassword) {
      message.error("密码未更改")
    }
    else if (newPassword !== confirmNewPassword) {
      message.error("两次输入的新密码不一致")
    }
    else {
      setUpdating(true)
      userService.updatePassword(data).then(res => {
        message.success("修改成功")
      }).catch(err => {
        handleValidationError(err, () => {
          showHttpErrorMessageVoid(err)
        })
      }).finally(() => setUpdating(false))
    }
  }
  return (
    <>
      <Form name="passwordForm" layout="vertical" onFinish={ update }>
        <FormItem name="oldPassword" label="原密码" rules={ passwordRules }>
          <Input.Password maxLength="48" placeholder="请输入当前账号密码" />
        </FormItem>
        <FormItem name="newPassword" label="新密码" rules={ newPasswordRules }>
          <Input.Password maxLength="48" placeholder="请输入新密码" />
        </FormItem>
        <FormItem name="confirmNewPassword" label="确认新密码" rules={ newPasswordRules }>
          <Input.Password maxLength="48" placeholder="请重复输入新密码" />
        </FormItem>
        <Button loading={ updating } type="primary" htmlType="submit">保存修改</Button>
      </Form>
    </>
  )
}

const AvatarSettings = ({ updating, setUpdating, userSession, setUserSession }) => {
  const [uploadFilename, setUploadFilename] = useState("avatar.png")

  const onFileSelected = file => {
    console.log(file)
    setUploadFilename(file.name)
  }

  const updateAvatar = data => {
    if (updating) {
      return
    }
    const formData = new FormData()
    formData.append('avatar', data, uploadFilename)
    setUpdating(true)
    userService.updateAvatar(formData).then(extractData).then(user => {
      setUserSession(user)
      message.success("更换成功")
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }

  return (
    <>
      <Alert message="禁止使用任何涉嫌非法或者敏感图片作为头像" type="warning" style={ { marginBottom: 15 } } />
      <ImageCropper preview=".avatar"
                    updating={ updating }
                    src={ userSession.avatar }
                    onSubmit={ updateAvatar }
                    onFileSelected={ onFileSelected }
                    aspectRatio={ 1 }>
        <div className="img-preview avatar preview-lg" />
        <div className="img-preview avatar preview-md" />
        <div className="img-preview avatar preview-sm" />
        <div className="img-preview avatar preview-xs" />
      </ImageCropper>
    </>
  )
}

export default props => {
  const { initialState, setInitialState } = useModel("@@initialState")
  const { currentUser: userSession, options } = initialState;

  const [updating, setUpdating] = useState(false)

  const setUserSession = async currentUser => {
    await setInitialState({ ...initialState, currentUser })
  }

  const newProps = { ...props, userSession, options, setUserSession, updating, setUpdating }

  return (<>
    <Spin spinning={ updating }>
      <Tabs type="line">
        <TabPane tab="资料修改" key="1">
          <InfoSettings { ...newProps } />
        </TabPane>

        <TabPane tab="邮箱 / 手机号修改" key="2">
          <EmailSettings { ...newProps } />
        </TabPane>

        <TabPane tab="密码修改" key="3">
          <PasswordSettings { ...newProps } />
        </TabPane>

        <TabPane tab="头像修改" key="4">
          <AvatarSettings { ...newProps } />
        </TabPane>
      </Tabs>
    </Spin>
  </>)
}
