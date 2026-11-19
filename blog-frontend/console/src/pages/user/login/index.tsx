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

import { LockOutlined, UserOutlined, } from '@ant-design/icons';
import { Alert, message, Popconfirm } from 'antd';
import React, { useState } from 'react';
import { LoginForm, ProFormCheckbox, ProFormText } from '@ant-design/pro-form';
import { FormattedMessage, history, useIntl, useModel } from '@umijs/max';
import Footer from '@/components/Footer';
import { login } from '@/services/ant-design-pro/login';

import styles from './index.less';
import logo from '@/assets/logo.svg';

import { getStorage, mergeValidationError, removeStorage, saveStorage } from "@/utils";

function getInitialValue() {
  return {
    email: getStorage("lastLogin"),
    remember: true
  }
}

const LoginMessage: React.FC<{ content?: string; }> = ({ content }) => (
    <Alert style={{ marginBottom: 24, }}
           message={content}
           type="error"
           showIcon
    />
);

/** 此方法会跳转到 redirect 参数所在的位置 */
const goto = () => {
  if (!history) return;
  setTimeout(() => {
    const redirect = new URLSearchParams(history.location.search).get('redirect');
    history.push(redirect?.startsWith('/') && !redirect.startsWith('//') ? redirect : '/');
  }, 10);
};

const Login: React.FC = () => {
  const [_, setSubmitting] = useState(false);
  const [userLoginState, setUserLoginState] = useState<API.LoginResult>({});
  const { initialState, setInitialState } = useModel('@@initialState');

  const intl = useIntl();
  const t = (zh: string, en: string) => intl.locale === 'en-US' ? en : zh;

  const setUserInfo = (userInfo: API.CurrentUser) => {
    if (userInfo) {
      setInitialState({
        ...initialState,
        currentUser: userInfo,
      });
    }
  };

  const handleSubmit = async (values: API.LoginParams) => {
    setSubmitting(true);
    try {
      // 登录
      const { remember } = values
      delete values['remember'] // delete remember value from the form-data
      login({ ...values }).then(res => {
        const user = res.data;
        if (user?.blogger) {
          message.success(t('登录成功！', 'Signed in.'));
          setUserInfo(user)

          if (remember) { // 记住邮箱
            saveStorage("lastLogin", values.email)
          }
          else {
            removeStorage("lastLogin")
          }

          goto()
        }
      }).catch(e => {
        // 展示错误信息
        setUserLoginState({
          success: false,
          message: e.message
        });
        message.error(e.message);
      })
    }
    catch (error) {
      mergeValidationError(error)
      setUserLoginState({ success: false, message: error.message || t('未知错误', 'Unknown error') });
    }
    finally {
      setSubmitting(false)
    }
  };
  const { success, message: errorMessage } = userLoginState;

  return (
      <div className={styles.container}>
        <div className={styles.content}>

          <LoginForm
              logo={logo}
              title={t('TODAY BLOG 后台管理', 'TODAY BLOG Admin')}
              subTitle={intl.formatMessage({ id: 'pages.layouts.userLayout.title' })}
              initialValues={getInitialValue()}
              onFinish={async (values) => {
                await handleSubmit(values as API.LoginParams);
              }}
              message={!success && errorMessage && (
                  <LoginMessage content={errorMessage}/>
              )}
          >

            {/*输入框*/}
            <>
              <ProFormText
                  name="email"
                  fieldProps={{
                    size: 'large',
                    prefix: <UserOutlined className={styles.prefixIcon}/>,
                  }}
                  placeholder={intl.formatMessage({
                    id: 'pages.login.username.placeholder',
                    defaultMessage: '请输入用户名',
                  })}
                  rules={[
                    {
                      required: true,
                      message: (
                          <FormattedMessage
                              id="pages.login.username.required"
                              defaultMessage="请输入用户名!"
                          />
                      ),
                    },
                  ]}
              />
              <ProFormText.Password
                  name="password"
                  fieldProps={{
                    size: 'large',
                    prefix: <LockOutlined className={styles.prefixIcon}/>,
                  }}
                  placeholder={intl.formatMessage({
                    id: 'pages.login.password.placeholder',
                    defaultMessage: '请输入密码',
                  })}
                  rules={[
                    {
                      required: true,
                      message: (
                          <FormattedMessage
                              id="pages.login.password.required"
                              defaultMessage="请输入密码！"
                          />
                      ),
                    },
                  ]}
              />
            </>
            {/*登录选项*/}
            <div style={{ marginBottom: 24 }}>
              <ProFormCheckbox noStyle name="remember">
                <FormattedMessage id="pages.login.rememberMe" defaultMessage="记住邮箱"/>
              </ProFormCheckbox>
              <Popconfirm title={t('暂不支持重置密码', 'Password reset is not available yet')}>
                <a style={{ float: 'right' }}>
                  <FormattedMessage id="pages.login.forgotPassword" defaultMessage="忘记密码"/>
                </a>
              </Popconfirm>
            </div>

          </LoginForm>

        </div>
        <Footer/>
      </div>
  );
};

export default Login;
