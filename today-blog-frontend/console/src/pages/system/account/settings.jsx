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

const passwordRules = (t, min) => [{ required: true, message: t('account.enterAPassword'), min, max: 48 }];

const InfoSettings = ({ updating, userSession, setUpdating, setUserSession }) => {
  const t = useSiteTranslation();
  const update = data => {
    if (updating) {
      return
    }
    setUpdating(true)
    userService.updateInfo(data).then(extractData).then(user => {
      setUserSession(user)
      message.success(t('account.changesSaved'))
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }

  return (<>
    <Form name="infoForm" layout="vertical" initialValues={{ ...userSession }} onFinish={update}>
      <FormItem name="name" label={t('account.nameOrNickname')} rules={[{ required: true, message: t('account.nameRequired') }]}>
        <Input maxLength="24" placeholder={t('account.enterANameOrNickname')}/>
      </FormItem>
      <FormItem name="introduce" label={t('account.bio')}>
        <Input maxLength="64" placeholder={t('account.enterAShortBio')}/>
      </FormItem>
      <Button loading={updating} type="primary" htmlType="submit">{t('account.saveChanges')}</Button>
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
      message.success(t('account.changesSaved'))
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }
  return (
    <>
      <Form name="emailForm" layout="vertical" initialValues={{ ...userSession }} onFinish={update}>
        <FormItem name="password" label={t('account.currentPassword')} rules={passwordRules(t)}>
          <Input.Password maxLength="24" placeholder={t('account.enterYourCurrentPassword')}/>
        </FormItem>

        <FormItem name="mobilePhone" label={t('account.newPhoneNumber')} rules={[
          { required: true, message: t('account.newPhoneRequired') },
          { pattern: /^1[3456789]\d{9}$/, message: t('account.enterAValidPhoneNumber') },
        ]}>
          <Input maxLength="11" placeholder={t('account.enterANewPhoneNumber')}/>
        </FormItem>

        <FormItem name="email" label={t('account.newEmailAddress')} rules={[
          { required: true, message: t('account.newEmailRequired') },
          { type: 'email', message: t('account.enterAValidEmailAddress') },
        ]}>
          <Input maxLength="48" placeholder={t('account.enterANewEmailAddress')}/>
        </FormItem>
        <Button loading={updating} type="primary" htmlType="submit">{t('account.saveChanges')}</Button>
      </Form>
    </>
  )
}

const PasswordSettings = ({ updating, setUpdating }) => {
  const t = useSiteTranslation();
  const update = data => {
    const { oldPassword, newPassword, confirmNewPassword } = data
    if (oldPassword === newPassword) {
      message.error(t('account.chooseADifferentPassword'))
    }
    else if (newPassword !== confirmNewPassword) {
      message.error(t('account.theNewPasswordsDoNot'))
    }
    else {
      setUpdating(true)
      userService.updatePassword(data).then(res => {
        message.success(t('account.passwordChanged'))
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
        <FormItem name="oldPassword" label={t('account.oldPassword')} rules={passwordRules(t)}>
          <Input.Password maxLength="48" placeholder={t('account.enterYourCurrentPassword')}/>
        </FormItem>
        <FormItem name="newPassword" label={t('account.newPassword')} rules={passwordRules(t, 2)}>
          <Input.Password maxLength="48" placeholder={t('account.enterANewPassword')}/>
        </FormItem>
        <FormItem name="confirmNewPassword" label={t('account.confirmNewPassword')} rules={passwordRules(t, 2)}>
          <Input.Password maxLength="48" placeholder={t('account.reEnterYourNewPassword')}/>
        </FormItem>
        <Button loading={updating} type="primary" htmlType="submit">{t('account.saveChanges')}</Button>
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
      message.success(t('account.avatarUpdated'))
    }).catch(showHttpErrorMessageVoid)
      .finally(() => setUpdating(false))
  }

  return (
    <>
      <Alert message={t('account.doNotUseUnlawfulOr')} type="warning" style={{ marginBottom: 15 }}/>
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
        <TabPane tab={t('account.profile')} key="1">
          <InfoSettings {...newProps} />
        </TabPane>

        <TabPane tab={t('account.emailAndPhone')} key="2">
          <EmailSettings {...newProps} />
        </TabPane>

        <TabPane tab={t('account.password')} key="3">
          <PasswordSettings {...newProps} />
        </TabPane>

        <TabPane tab={t('account.editAvatar')} key="4">
          <AvatarSettings {...newProps} />
        </TabPane>
      </Tabs>
    </Spin>
  </>)
}
