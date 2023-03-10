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

import { Button, Checkbox, Form, Input, message } from 'antd';
import React from 'react';
import { ImageCropper } from '../';
import { userService } from '../../services';
import { scrollTop } from '../../utils';
import { connect } from "react-redux";
import { userSessionOptionsMapStateToProps } from "../../redux/action-types";
import { updateUserSession } from "../../redux/actions";

const emailRules = [
  { required: true, message: '请输入您的邮箱!' },
  { type: 'email', message: '请您输入正确格式的邮箱!' }
]
const passwordRules = [
  { required: true, message: '请输入密码!' }
]
const nameRules = [
  { required: true, message: '请输入姓名或昵称!' }
]

function handleError(err) {
  message.error(err.message)
}

function handleResult(props, res) {
  if (res.data.success === true) {
    message.success(res.data.message)
    props.updateUserSession(res.data.data)
  }
  else {
    message.error(res.data.message)
  }
}

function InfoSettings(props) {
  const update = (data) => {
    userService
      .updateInfo(data)
      .then(res => handleResult(props, res))
      .catch(handleError)
  }

  return (<>
    <div className="data_list" id="info">
      <div className="data_list_title">资料修改</div>
      <div className="data content">
        <Form name="infoForm" layout="vertical" initialValues={{ ...props.userSession }} onFinish={update}>
          <Form.Item name="name" label='姓名或昵称' rules={nameRules}>
            <Input maxLength="24"/>
          </Form.Item>
          <Form.Item name="site" label='个人网站'>
            <Input maxLength="48"/>
          </Form.Item>
          <Form.Item name="introduce" label='自我描述' rules={passwordRules}>
            <Input maxLength="48"/>
          </Form.Item>
          {props.options['comment.send.mail'] === true &&
            // userSession.notification
            <Form.Item name="notification" valuePropName="checked" noStyle>
              <Checkbox>邮件通知</Checkbox>
              <small>*当有评论回复，评论审核通过时系统会发送邮件通知您。</small>
            </Form.Item>
          }
          <Button type="primary" htmlType="submit" title="保存修改">保存修改</Button>
        </Form>
      </div>
    </div>
  </>)
}

function EmailSettings(props) {
  const update = (data) => {
    userService
      .updateEmail(data)
      .then(res => handleResult(props, res))
      .catch(handleError)
  }
  return (
    <div className="data_list" id="email">
      <div className="data_list_title">邮箱修改</div>
      <div className="data content">
        <Form name="emailForm" layout="vertical" initialValues={{ ...props.userSession }} onFinish={update}>
          <Form.Item name="password" label='密码' rules={passwordRules}>
            <Input.Password maxLength="24"/>
          </Form.Item>
          <Form.Item name="email" label='新邮箱' rules={emailRules}>
            <Input maxLength="48"/>
          </Form.Item>
          <Button type="primary" htmlType="submit" title="保存修改">保存修改</Button>
        </Form>
      </div>
    </div>
  )
}

function PasswordSettings(props) {
  const update = (data) => {
    userService
      .updatePassword(data)
      .then(res => {
        if (res.data.success === true) {
          message.success(res.data.message)
        }
        else {
          message.error(res.data.message)
        }
      })
      .catch(handleError)
  }
  return (
    <div className="data_list" id="password">
      <div className="data_list_title">密码修改</div>
      <div className="data content">
        <Form name="passwordForm" layout="vertical" onFinish={update}>
          <Form.Item name="password" label='原密码' rules={passwordRules}>
            <Input maxLength="48"/>
          </Form.Item>
          <Form.Item name="newPassword" label='新密码' rules={passwordRules}>
            <Input.Password maxLength="48"/>
          </Form.Item>
          <Form.Item name="rePassword" label='确认新密码' rules={passwordRules}>
            <Input.Password maxLength="48"/>
          </Form.Item>
          <Button type="primary" htmlType="submit" title="保存修改">保存修改</Button>
        </Form>
      </div>
    </div>
  )
}

class AvatarSettings extends React.Component {

  updateAvatar = (data) => {
    const formData = new FormData()
    formData.append('avatar', data, 'avatar.png')
    userService
      .updateAvatar(formData)
      .then(res => {
        if (res.data) {
          scrollTop()
          message.success(res.data.message)
          this.props.updateUserSession(Object.assign({}, this.props.userSession, { image: res.data.data }))
        }
        else {
          message.error("更换失败")
        }
      })
      .catch(handleError)
  }

  render() {
    const { userSession } = this.props
    return (
      <div className="data_list" id="avatar">
        <div className="data_list_title">头像修改(禁止使用任何涉嫌非法或者敏感图片作为头像)</div>
        <div className="data content">
          <ImageCropper onCut={this.updateAvatar}
                        src={userSession.avatar} preview='.avatar' aspectRatio={1}>
            <div className="img-preview avatar preview-lg"/>
            <div className="img-preview avatar preview-md"/>
            <div className="img-preview avatar preview-sm"/>
            <div className="img-preview avatar preview-xs"/>
          </ImageCropper>
        </div>
      </div>
    )
  }
}


function BackgroundSettings(props) {

  const { userSession } = props

  const updateBackground = (data) => {
    const formData = new FormData()
    formData.append('background', data, 'background.png')
    userService
      .updateBackground(formData)
      .then(res => {
        if (res.data) {
          scrollTop()
          message.success(res.data.message)
          props.updateUserSession(Object.assign({}, userSession, { background: res.data.data }))
        }
        else {
          message.error("更换失败")
        }
      })
      .catch(handleError)
  }
  return (
    <div className="data_list" id="background">
      <div className="data_list_title">主页背景修改</div>
      <div className="data content">
        <ImageCropper onCut={updateBackground} src={userSession.background} preview='.background' aspectRatio={12 / 6}>
          <div className="img-preview background preview-lg"/>
        </ImageCropper>
      </div>
    </div>
  )
}

class Settings extends React.Component {

  state = {
    loaded: false,
    comments: {
      all: 0,
      size: 10,
      data: [],
      current: 1
    }
  }

  componentDidMount() {
  }

  render() {
    const props = this.props;
    return (<>
      <InfoSettings {...props} />
      <EmailSettings {...props} />
      <PasswordSettings {...props} />
      <AvatarSettings {...props} />
      <BackgroundSettings {...props} />
    </>)
  }
}

export default connect(
  userSessionOptionsMapStateToProps, { updateUserSession }
)(Settings)
