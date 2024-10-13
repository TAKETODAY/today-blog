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

import { LockOutlined, UserOutlined, } from '@ant-design/icons';
import { Alert, message, Popconfirm } from 'antd';
import React, { useState } from 'react';
import { LoginForm, ProFormCheckbox, ProFormText } from '@ant-design/pro-form';
import { FormattedMessage, history, SelectLang, useIntl, useModel } from 'umi';
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
    const { query } = history.location;
    const { redirect } = query as { redirect: string };
    history.push(redirect || '/');
  }, 10);
};

const Login: React.FC = () => {
  const [_, setSubmitting] = useState(false);
  const [userLoginState, setUserLoginState] = useState<API.LoginResult>({});
  const { initialState, setInitialState } = useModel('@@initialState');

  const intl = useIntl();

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
      const ret = await login({ ...values });
      const { data } = ret
      if (data.success) {
        const user = data.data
        if (user?.blogger) {
          message.success('登录成功！');
          setUserInfo(user)

          if (remember) { // 记住邮箱
            saveStorage("lastLogin", values.email)
          }
          else {
            removeStorage("lastLogin")
          }

          goto();
        }
        return;
      }
      else {
        // 如果失败去设置用户错误信息
        setUserLoginState(ret);
        message.error(data.message);
      }
    }
    catch (error) {
      mergeValidationError(error)
      setUserLoginState({ success: false, message: error.message ? error.message : "未知错误" });
    }
    finally {
      setSubmitting(false)
    }
  };
  const { success, message: errorMessage } = userLoginState;

  return (
      <div className={styles.container}>
        <div className={styles.lang}>{SelectLang && <SelectLang/>}</div>
        <div className={styles.content}>

          <LoginForm
              logo={logo}
              title="TODAY BLOG 后台管理"
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
              <Popconfirm title="还不支持">
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
