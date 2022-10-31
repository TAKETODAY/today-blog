import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Checkbox, Form, Input, message, Popconfirm } from 'antd';
import React from 'react';
import github from '../assets/images/login/github.png';
import gitee from '../assets/images/login/gitee.png';
import { userService } from '../services';
import { getStorage, saveStorage, removeStorage } from '../utils';
import { Link } from "react-router-dom";
import { Image } from './'
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import { connect } from "react-redux";
import { Alert } from 'antd';

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
    const { userSession, forward, message } = this.props
    return (<>
      { userSession ?
          <>
            <div className="login_avatar">
              <h2>已登录</h2>
              <Link to="/user/info" title={ userSession.name }>
                <Image alt="头像" className="img-responsive info_avatar" src={ userSession.image }/>
              </Link>
            </div>
            <Button type="primary" onClick={ this.loginOther } title="登录其他账号" block>登录其他账号</Button>
          </> :
          <>
            <h2 style={ { margin: '10px 10px 25px 10px', textAlign: 'center' } }>欢迎登录</h2>
            { message && <Alert message={ message } showIcon type="error" style={ { marginBottom: 10 } }/> }
            <Form name="login" initialValues={ getInitialValue() } onFinish={ this.login }>
              <Form.Item name="email" rules={ emailRules }>
                <Input size="large" placeholder="邮箱" name="email" prefix={ <UserOutlined/> }/>
              </Form.Item>
              <Form.Item name="password" rules={ passwordRules }>
                <Input.Password size="large" placeholder="密码" name="passwd" prefix={ <LockOutlined/> }/>
              </Form.Item>
              <Form.Item>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>记住邮箱</Checkbox>
                </Form.Item>
                <Popconfirm title="还不支持该功能">
                  <a style={ { float: 'right' } }>忘记密码</a>
                </Popconfirm>
              </Form.Item>
              <Button type="primary" htmlType="submit" title="点击登录" block>登录</Button>
            </Form>
          </>
      }
      <div style={ { textAlign: 'center', marginTop: '35px' } }>
        <div style={ { borderBottom: '1px solid #eee', margin: '0px 0 5px 0', position: 'relative', top: '-10px' } }>
          <a style={ { position: 'relative', top: '10px', padding: '0 10px', background: '#fff' } }>更多登录方式</a>
        </div>

        {/*<a href={`/api/auth/github${forward ? `?forward=${encodeURIComponent(forward)}` : ''}`}>*/ }
        <a style={ { padding: 5 } } href={ `/api/auth/github${ forward ? `?forward=${ encodeURIComponent(forward) }` : '' }` }>
          <img alt="GitHub登录" title="GitHub登录" src={ github } width="22"/>
        </a>
        <a style={ { padding: 5 } } href={ `/api/auth/gitee${ forward ? `?forward=${ encodeURIComponent(forward) }` : '' }` }>
          <img alt="Gitee登录" title="Gitee登录" src={ gitee } width="25"/>
        </a>
      </div>
    </>)
  }

}

export default connect(userSessionMapStateToProps, {
  updateUserSession
})(Login)
