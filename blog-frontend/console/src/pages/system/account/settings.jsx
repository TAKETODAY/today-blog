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

import { Alert, Button, Form, Input, message, Spin, Tabs } from 'antd';
import React, { useState } from 'react';
import ImageCropper from './cropper/ImageCropper';
import UserService from "./service"
import { extractData, handleValidationError, showHttpErrorMessageVoid } from "@/utils"
import { useOptions, useUserSession } from "@/components/hooks";
import { useSiteTranslation } from '@/utils/use-site-translation';

const FormItem = Form.Item
const { TabPane } = Tabs;

const userService = new UserService()

const passwordRules = (t, min) => [{ required: true, message: t('请输入密码!', 'Enter a password'), min, max: 48 }];

const InfoSettings = ({ updating, userSession, setUpdating, setUserSession }) => {
  const t = useSiteTranslation();
  const update = data => {
    if (updating) {
      return
    }
    setUpdating(true)
    userService.updateInfo(data).then(extractData).then(user => {
      setUserSession(user)
      message.success(t('修改成功', 'Changes saved'))
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }

  return (<>
    <Form name="infoForm" layout="vertical" initialValues={{ ...userSession }} onFinish={update}>
      <FormItem name="name" label={t('姓名或昵称', 'Name or nickname')} rules={[{ required: true, message: t('请输入姓名或昵称!', 'Enter a name or nickname') }]}>
        <Input maxLength="24" placeholder={t('请输入姓名或昵称', 'Enter a name or nickname')}/>
      </FormItem>
      <FormItem name="introduce" label={t('自我描述', 'Bio')}>
        <Input maxLength="64" placeholder={t('请输入自我描述', 'Enter a short bio')}/>
      </FormItem>
      <Button loading={updating} type="primary" htmlType="submit">{t('保存修改', 'Save changes')}</Button>
    </Form>
  </>)
}

const EmailSettings = ({ updating, userSession, setUpdating, setUserSession }) => {
  const t = useSiteTranslation();
  const update = data => {
    if (updating) {
      return
    }
    setUpdating(true)
    userService.updateEmailAndMobile(data).then(extractData).then(user => {
      setUserSession(user)
      message.success(t('修改成功', 'Changes saved'))
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }
  return (
    <>
      <Form name="emailForm" layout="vertical" initialValues={{ ...userSession }} onFinish={update}>
        <FormItem name="password" label={t('当前账号密码', 'Current password')} rules={passwordRules(t)}>
          <Input.Password maxLength="24" placeholder={t('请输入当前账号密码', 'Enter your current password')}/>
        </FormItem>

        <FormItem name="mobilePhone" label={t('新手机号', 'New phone number')} rules={[
          { required: true, message: t('请输入新的手机号!', 'Enter a new phone number') },
          { pattern: /^1[3456789]\d{9}$/, message: t('请您输入正确格式的手机号!', 'Enter a valid phone number') },
        ]}>
          <Input maxLength="11" placeholder={t('请输入新手机号', 'Enter a new phone number')}/>
        </FormItem>

        <FormItem name="email" label={t('新邮箱', 'New email address')} rules={[
          { required: true, message: t('请输入新的邮箱!', 'Enter a new email address') },
          { type: 'email', message: t('请您输入正确格式的邮箱!', 'Enter a valid email address') },
        ]}>
          <Input maxLength="48" placeholder={t('请输入新邮箱', 'Enter a new email address')}/>
        </FormItem>
        <Button loading={updating} type="primary" htmlType="submit">{t('保存修改', 'Save changes')}</Button>
      </Form>
    </>
  )
}

const PasswordSettings = ({ updating, setUpdating }) => {
  const t = useSiteTranslation();
  const update = data => {
    const { oldPassword, newPassword, confirmNewPassword } = data
    if (oldPassword === newPassword) {
      message.error(t('密码未更改', 'Choose a different password'))
    }
    else if (newPassword !== confirmNewPassword) {
      message.error(t('两次输入的新密码不一致', 'The new passwords do not match'))
    }
    else {
      setUpdating(true)
      userService.updatePassword(data).then(res => {
        message.success(t('修改成功', 'Password changed'))
      }).catch(err => {
        handleValidationError(err, () => {
          showHttpErrorMessageVoid(err)
        })
      }).finally(() => setUpdating(false))
    }
  }
  return (
    <>
      <Form name="passwordForm" layout="vertical" onFinish={update}>
        <FormItem name="oldPassword" label={t('原密码', 'Current password')} rules={passwordRules(t)}>
          <Input.Password maxLength="48" placeholder={t('请输入当前账号密码', 'Enter your current password')}/>
        </FormItem>
        <FormItem name="newPassword" label={t('新密码', 'New password')} rules={passwordRules(t, 2)}>
          <Input.Password maxLength="48" placeholder={t('请输入新密码', 'Enter a new password')}/>
        </FormItem>
        <FormItem name="confirmNewPassword" label={t('确认新密码', 'Confirm new password')} rules={passwordRules(t, 2)}>
          <Input.Password maxLength="48" placeholder={t('请重复输入新密码', 'Re-enter your new password')}/>
        </FormItem>
        <Button loading={updating} type="primary" htmlType="submit">{t('保存修改', 'Save changes')}</Button>
      </Form>
    </>
  )
}

const AvatarSettings = ({ updating, setUpdating, userSession, setUserSession }) => {
  const t = useSiteTranslation();
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
      message.success(t('更换成功', 'Avatar updated'))
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }

  return (
    <>
      <Alert message={t('禁止使用任何涉嫌非法或者敏感图片作为头像', 'Do not use unlawful or sensitive images as your avatar')} type="warning" style={{ marginBottom: 15 }}/>
      <ImageCropper preview=".avatar"
                    updating={updating}
                    src={userSession.avatar}
                    onSubmit={updateAvatar}
                    onFileSelected={onFileSelected}
                    aspectRatio={1}>
        <div className="img-preview avatar preview-lg"/>
        <div className="img-preview avatar preview-md"/>
        <div className="img-preview avatar preview-sm"/>
        <div className="img-preview avatar preview-xs"/>
      </ImageCropper>
    </>
  )
}

export default props => {
  const t = useSiteTranslation();
  const [options] = useOptions()
  const [userSession, setUserSession] = useUserSession()
  const [updating, setUpdating] = useState(false)
  const newProps = { ...props, userSession, options, setUserSession, updating, setUpdating }

  return (<>
    <Spin spinning={updating}>
      <Tabs type="line">
        <TabPane tab={t('资料修改', 'Profile')} key="1">
          <InfoSettings {...newProps} />
        </TabPane>

        <TabPane tab={t('邮箱 / 手机号修改', 'Email and phone')} key="2">
          <EmailSettings {...newProps} />
        </TabPane>

        <TabPane tab={t('密码修改', 'Password')} key="3">
          <PasswordSettings {...newProps} />
        </TabPane>

        <TabPane tab={t('头像修改', 'Avatar')} key="4">
          <AvatarSettings {...newProps} />
        </TabPane>
      </Tabs>
    </Spin>
  </>)
}
