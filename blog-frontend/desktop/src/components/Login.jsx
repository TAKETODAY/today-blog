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

import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Checkbox, Form, Input, message, Popconfirm } from 'antd';
import React from 'react';
import { userService } from '../services';
import { getStorage, removeStorage, saveStorage } from 'core';
import { Link } from "react-router-dom";
import { Image } from './'
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import { connect } from "react-redux";

const emailRules = [
  { required: true, message: '请输入您的邮箱!' },
  { type: 'email', message: '请您输入正确格式的邮箱!' }
]
const passwordRules = [
  { required: true, message: '请输入密码!' }
]

function getInitialValue() {
  return {
    email: getStorage("lastLogin"),
    remember: true
  }
}

class Login extends React.PureComponent {

  login = fromData => {
    const remember = fromData['remember']
    delete fromData['remember'] // delete remember value from the form-data
    userService.login(fromData).then(res => {
      const result = res.data
      if (result.success) { // 登陆成功
        if (remember) { // 记住邮箱
          saveStorage("lastLogin", fromData["email"])
        }
        else {
          removeStorage("lastLogin")
        }
        message.success(result.message)
        this.props.updateUserSession(result.data)
      }
      else {
        message.error(result.message)
      }
    }).catch(res => {
      message.error(res.message)
    })
  }

  loginOther = () => {
    userService.logout().then(res => {
      message.info('登出成功!')
      this.props.updateUserSession(null)
    }).catch(resp => {
      message.error('登出失败!')
    })
  }

  render() {
    const { userSession, message } = this.props
    return (<>
      {userSession ?
        <>
          <div className="login_avatar">
            <h2>已登录</h2>
            <Link to="/user/info" title={userSession.name}>
              <Image alt="头像" className="img-responsive info_avatar" src={userSession.avatar}/>
            </Link>
          </div>
          <Popconfirm title="确认先退出再登录其他账号吗？" onConfirm={this.loginOther}>
            <Button type="primary" title="登录其他账号" block>登录其他账号</Button>
          </Popconfirm>
        </> :
        <>
          <h2 style={{ margin: '10px 10px 25px 10px', textAlign: 'center' }}>欢迎登录</h2>
          {message && <Alert message={message} showIcon type="error" style={{ marginBottom: 10 }}/>}
          <Form name="login" initialValues={getInitialValue()} onFinish={this.login}>
            <Form.Item name="email" rules={emailRules}>
              <Input size="large" placeholder="邮箱" name="email" prefix={<UserOutlined/>}/>
            </Form.Item>
            <Form.Item name="password" rules={passwordRules}>
              <Input.Password size="large" placeholder="密码" name="passwd" prefix={<LockOutlined/>}/>
            </Form.Item>
            <Form.Item>
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>记住邮箱</Checkbox>
              </Form.Item>
              <Popconfirm title="还不支持该功能">
                <a style={{ float: 'right' }}>忘记密码</a>
              </Popconfirm>
            </Form.Item>
            <Button type="primary" htmlType="submit" title="点击登录" block>登录</Button>
          </Form>
        </>
      }
    </>)
  }

}

export default connect(userSessionMapStateToProps, {
  updateUserSession
})(Login)
