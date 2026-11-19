/*
 * Copyright 2017 - 2025 the original author or authors.
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
import 'antd/es/alert/style';
import 'antd/es/button/style';
import 'antd/es/checkbox/style';
import 'antd/es/form/style';
import 'antd/es/input/style';
import 'antd/es/message/style';
import 'antd/es/popconfirm/style';
import React from 'react';
import { userService } from '../services';
import { getStorage, removeStorage, saveStorage, translate } from 'core';
import Image from './Image'
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import { connect } from "react-redux";
import { useDisplayPreferences } from './hooks/useDisplayPreferences';

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
      if (remember) { // 记住邮箱
        saveStorage("lastLogin", fromData["email"])
      }
      else {
        removeStorage("lastLogin")
      }
      this.props.updateUserSession(res.data)
    }).catch(res => {
      message.error(res.message)
    })
  }

  loginOther = () => {
    userService.logout().then(res => {
      message.info(translate('登出成功！', 'Signed out.'))
      this.props.updateUserSession(null)
    }).catch(resp => {
      message.error(translate('登出失败！', 'Could not sign out.'))
    })
  }

  render() {
    const { userSession, message } = this.props
    return (<>
      {userSession ?
        <>
          <div className="login_avatar">
            <h2>{translate('已登录', 'Signed in')}</h2>
            <Image alt={translate('头像', 'Avatar')} className="img-responsive info_avatar" src={userSession.avatar}/>
          </div>
          <Popconfirm title={translate('确认先退出再登录其他账号吗？', 'Sign out to use another account?')} onConfirm={this.loginOther}>
            <Button type="primary" block>{translate('登录其他账号', 'Use another account')}</Button>
          </Popconfirm>
        </> :
        <>
          <h2 style={{ margin: '10px 10px 25px 10px', textAlign: 'center' }}>{translate('欢迎登录', 'Sign in')}</h2>
          {message && <Alert message={message} showIcon type="error" style={{ marginBottom: 10 }}/>}
          <Form name="login" initialValues={getInitialValue()} onFinish={this.login}>
            <Form.Item name="email" rules={[
              { required: true, message: translate('请输入您的邮箱！', 'Enter your email address.') },
              { type: 'email', message: translate('请输入正确格式的邮箱！', 'Enter a valid email address.') },
            ]}>
              <Input size="large" placeholder={translate('邮箱', 'Email')} name="email" prefix={<UserOutlined/>}/>
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: translate('请输入密码！', 'Enter your password.') }]}>
              <Input.Password size="large" placeholder={translate('密码', 'Password')} name="passwd" prefix={<LockOutlined/>}/>
            </Form.Item>
            <Form.Item>
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>{translate('记住邮箱', 'Remember email')}</Checkbox>
              </Form.Item>
              <Popconfirm title={translate('还不支持该功能', 'Password reset is not available yet.')}>
                <a style={{ float: 'right' }}>{translate('忘记密码', 'Forgot password?')}</a>
              </Popconfirm>
            </Form.Item>
            <Button type="primary" htmlType="submit" block>{translate('登录', 'Sign in')}</Button>
          </Form>
        </>
      }
    </>)
  }

}

const ConnectedLogin = connect(userSessionMapStateToProps, {
  updateUserSession
})(Login)

export default function LocalizedLogin(props) {
  const displayPreferences = useDisplayPreferences();
  return <ConnectedLogin {...props} displayPreferences={displayPreferences}/>;
}
