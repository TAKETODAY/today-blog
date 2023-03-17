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

import type { Settings as LayoutSettings } from '@ant-design/pro-layout';
import { PageLoading } from '@ant-design/pro-layout';
import { Modal, notification } from 'antd';
import type { RequestConfig, RunTimeLayoutConfig } from 'umi';
import { history } from 'umi';
import RightContent from '@/components/RightContent';
import Footer from '@/components/Footer';
import type { ResponseError } from 'umi-request';
import { currentUser as queryCurrentUser } from './services/ant-design-pro/api';
import { getCacheable } from "@/utils";

/** 获取用户信息比较慢的时候会展示一个 loading */
export const initialStateConfig = {
  loading: <PageLoading/>,
};

/**
 * @see https://umijs.org/zh-CN/plugins/plugin-initial-state
 */
export async function getInitialState(): Promise<{
  options?: { [key: string]: any };
  settings?: Partial<LayoutSettings>;
  currentUser?: API.CurrentUser;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
}> {
  const fetchUserInfo = async () => {
    try {
      let res = await queryCurrentUser()
      return res.data
    }
    catch (error) {
      history.push('/user/login');
    }
    return undefined;
  };

  // @ts-ignore
  const options = await getCacheable("/api/options").then(res => res.data).catch(() => ({
    'site.icp': '',
    'site.copyright': 'TODAY & 2017 - 2023 All Rights Reserved.',
    'site.otherFooter': ''
  }))
  // 如果是登录页面，不执行
  if (history.location.pathname !== '/user/login') {
    const currentUser = await fetchUserInfo();
    return {
      options,
      currentUser,
      settings: {},
      fetchUserInfo,
    };
  }
  return {
    options,
    settings: {},
    fetchUserInfo,
  };
}

// https://umijs.org/zh-CN/plugins/plugin-layout
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    rightContentRender: () => <RightContent/>,
    disableContentMargin: false,
    footerRender: () => <Footer/>,
    onPageChange: () => {
      const { location } = history;
      // 如果没有登录，重定向到 login
      if (!initialState?.currentUser && location.pathname !== '/user/login') {
        history.push('/user/login');
      }
    },
    links: [
      <>
        {/*        <LinkOutlined/>
        <span onClick={() => {
              window.open('/umi/plugin/openapi');
            }}
        >
          openAPI 文档
        </span>
      </>,
      <>
        <BookOutlined/>
        <span
            onClick={() => {
              window.open('/~docs');
            }}
        >
          业务组件文档
        </span>*/}
      </>,
    ],
    menuHeaderRender: undefined,
    // 自定义 403 页面
    // unAccessible: <div>unAccessible</div>,
    ...initialState?.settings,
  };
};

const codeMessage = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '用户没有权限（令牌、用户名、密码错误）。',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  405: '请求方法不被允许。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
};

function showLoginDialog() {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title: "该操作需要登录",
      content: <span style={{ color: "red" }}>系统检测到: 您尚未登陆,或者在线时间超时<br/>
              <strong>如果有数据未保存请在请窗口打开系统登录</strong>
            </span>,
      cancelText: '取消',
      okText: '去登录',
      onOk: () => {
        resolve(null)
      },
      onCancel: async () => {
        reject()
      },
    })
  })
}

/** 异常处理程序
 * @see https://beta-pro.ant.design/docs/request-cn
 */
const errorHandler = (error: ResponseError) => {
  const { response } = error;
  console.log(response)
  if (response && response.status) {
    if (response.status === 401) {
      const { pathname } = location
      setTimeout(() => {
        if (pathname !== '/' && pathname !== '/user/login') {
          showLoginDialog().then(() => {
            if (location.pathname !== '/user/login') {
              history.push('/user/login?redirect=' + encodeURI(location.pathname))
            }
          }).catch(() => {

          })
        }
      }, 300)
      return
    }
    else {
      const { status, url } = response;
      const errorText = codeMessage[response.status] || response.statusText;

      notification.error({
        message: `请求错误 ${status}: ${url}`,
        description: errorText,
      });
    }
  }

  throw { ...error, ...error.data };
};

// https://umijs.org/zh-CN/plugins/plugin-request
export const request: RequestConfig = {
  errorHandler,
};
