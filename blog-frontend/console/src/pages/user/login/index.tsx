import { AlipayCircleOutlined, LockOutlined, TaobaoCircleOutlined, UserOutlined, WeiboCircleOutlined, } from '@ant-design/icons';
import { LoginForm, ProFormCheckbox, ProFormText, } from '@ant-design/pro-components';
import { FormattedMessage, Helmet, SelectLang, useIntl, useModel, } from '@umijs/max';
import { Alert, App } from 'antd';
import { createStyles } from 'antd-style';
import React, { useState } from 'react';
import Settings from '../../../../config/defaultSettings';
import { login } from '@/services/ant-design-pro/login';


import { getStorage, mergeValidationError, removeStorage, saveStorage } from "@/utils";

function getInitialValue() {
  return {
    email: getStorage("lastLogin"),
    remember: true
  }
}

const useStyles = createStyles(({ token }) => {
  return {
    action: {
      marginLeft: '8px',
      color: 'rgba(0, 0, 0, 0.2)',
      fontSize: '24px',
      verticalAlign: 'middle',
      cursor: 'pointer',
      transition: 'color 0.3s',
      '&:hover': {
        color: token.colorPrimaryActive,
      },
    },
    lang: {
      width: 42,
      height: 42,
      lineHeight: '42px',
      position: 'fixed',
      right: 16,
      borderRadius: token.borderRadius,
      ':hover': {
        backgroundColor: token.colorBgTextHover,
      },
    },
    container: {
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'auto',
      backgroundImage:
          "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
      backgroundSize: '100% 100%',
    },
  };
});

const ActionIcons = () => {
  const { styles } = useStyles();

  return (
      <>
        <AlipayCircleOutlined
            key="AlipayCircleOutlined"
            className={styles.action}
        />
        <TaobaoCircleOutlined
            key="TaobaoCircleOutlined"
            className={styles.action}
        />
        <WeiboCircleOutlined
            key="WeiboCircleOutlined"
            className={styles.action}
        />
      </>
  );
};

const Lang = () => {
  const { styles } = useStyles();

  return (
      <div className={styles.lang} data-lang>
        {SelectLang && <SelectLang/>}
      </div>
  );
};

const LoginMessage: React.FC<{
  content: string;
}> = ({ content }) => {
  return (
      <Alert
          style={{
            marginBottom: 24,
          }}
          title={content}
          type="error"
          showIcon
      />
  );
};

/** 此方法会跳转到 redirect 参数所在的位置 */
const goto = () => {
  setTimeout(() => {
    const urlParams = new URL(window.location.href).searchParams;
    window.location.href = urlParams.get('redirect') || '/';
  }, 10);
};

const Login: React.FC = () => {
  const [_, setSubmitting] = useState(false);
  const [userLoginState, setUserLoginState] = useState<API.LoginResult>({});
  const { initialState, setInitialState } = useModel('@@initialState');
  const { styles } = useStyles();
  const { message } = App.useApp();
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
      login({ ...values }).then(res => {
        const user = res.data;
        if (user?.blogger) {
          message.success('登录成功！');
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
      setUserLoginState({ success: false, message: error.message ? error.message : "未知错误" });
    }
    finally {
      setSubmitting(false)
    }
  };

  const { success, message: errorMessage } = userLoginState;

  return (
      <div className={styles.container}>
        <Helmet>
          <title>
            {intl.formatMessage({
              id: 'menu.login',
              defaultMessage: '登录页',
            })}
            {Settings.title && ` - ${Settings.title}`}
          </title>
        </Helmet>
        <Lang/>
        <div
            style={{
              flex: '1',
              padding: '32px 0',
            }}
        >
          <LoginForm contentStyle={{ minWidth: 280, maxWidth: '75vw', }}
                     logo={<img alt="logo" src="/logo.svg"/>}
                     title="TODAY BLOG 后台管理"
                     subTitle={intl.formatMessage({ id: 'pages.layouts.userLayout.title' })}

                     initialValues={getInitialValue()}
                     actions={[
                       <FormattedMessage
                           key="loginWith"
                           id="pages.login.loginWith"
                           defaultMessage="其他登录方式"
                       />,
                       <ActionIcons key="icons"/>,
                     ]}
                     onFinish={async (values) => {
                       await handleSubmit(values as API.LoginParams);
                     }}

                     message={!success && errorMessage && (
                         <LoginMessage content={errorMessage}/>
                     )}
          >

            <>
              <ProFormText
                  name="email"
                  fieldProps={{
                    size: 'large',
                    prefix: <UserOutlined/>,
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
                    prefix: <LockOutlined/>,
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

            <div style={{ marginBottom: 24, }}>
              <ProFormCheckbox noStyle name="autoLogin">
                <FormattedMessage
                    id="pages.login.rememberMe"
                    defaultMessage="自动登录"
                />
              </ProFormCheckbox>
              <a style={{ float: 'right', }}>
                <FormattedMessage id="pages.login.forgotPassword" defaultMessage="忘记密码"/>
              </a>
            </div>
          </LoginForm>
        </div>
      </div>
  );
};

export default Login;
