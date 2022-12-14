import { AlipayCircleOutlined, LockOutlined, TaobaoCircleOutlined, UserOutlined, WeiboCircleOutlined, } from '@ant-design/icons';
import { Alert, message, Space } from 'antd';
import React, { useState } from 'react';
import ProForm, { ProFormCheckbox, ProFormText } from '@ant-design/pro-form';
import { FormattedMessage, history, Link, SelectLang, useIntl, useModel } from 'umi';
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
  const [submitting, setSubmitting] = useState(false);
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
          <div className={styles.top}>
            <div className={styles.header}>
              <Link to="/">
                <img alt="logo" className={styles.logo} src={logo}/>
                <span className={styles.title}>TODAY BLOG</span>
              </Link>
            </div>
            <div className={styles.desc}>TODAY BLOG 代码是我心中一首诗</div>
          </div>

          <div className={styles.main}>
            <ProForm
                initialValues={getInitialValue()}
                submitter={{
                  searchConfig: {
                    submitText: intl.formatMessage({
                      id: 'pages.login.submit',
                      defaultMessage: '登录',
                    }),
                  },
                  render: (_, dom) => dom.pop(),
                  submitButtonProps: {
                    loading: submitting,
                    size: 'large',
                    style: {
                      width: '100%',
                    },
                  },
                }}
                onFinish={async (values) => {
                  handleSubmit(values as API.LoginParams);
                }}
            >
              {!success && errorMessage && (
                  <LoginMessage content={errorMessage}/>
              )}

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

              <div style={{ marginBottom: 24 }}>
                <ProFormCheckbox noStyle name="remember">
                  <FormattedMessage id="pages.login.rememberMe" defaultMessage="记住邮箱"/>
                </ProFormCheckbox>
                <a style={{ float: 'right' }}>
                  <FormattedMessage id="pages.login.forgotPassword" defaultMessage="忘记密码"/>
                </a>
              </div>
            </ProForm>
            <Space className={styles.other}>
              <FormattedMessage id="pages.login.loginWith" defaultMessage="其他登录方式"/>
              <AlipayCircleOutlined className={styles.icon}/>
              <TaobaoCircleOutlined className={styles.icon}/>
              <WeiboCircleOutlined className={styles.icon}/>
            </Space>
          </div>
        </div>
        <Footer/>
      </div>
  );
};

export default Login;
