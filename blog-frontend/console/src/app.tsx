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

import { history, RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { PageLoading, SettingDrawer, Settings as LayoutSettings } from '@ant-design/pro-components';

import { AvatarDropdown, AvatarName, Footer, SelectLang } from '@/components';
import { extractData, getCacheable } from "@/utils";
import { currentUser as queryCurrentUser } from './services/ant-design-pro/api';
import Forbidden from "@/pages/403";


const isDev = process.env.NODE_ENV === 'development';
const isDevOrTest = isDev || process.env.CI;
const loginPath = '/user/login';


/**
 * @see https://umijs.org/zh-CN/plugins/plugin-initial-state
 */
export async function getInitialState(): Promise<{
  options?: { [key: string]: any }
  settings?: Partial<LayoutSettings>
  currentUser?: API.CurrentUser
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>
}> {
  const fetchUserInfo = async () => {
    return queryCurrentUser().then(extractData);
  }

  const options = await getCacheable("/api/options").then(extractData).catch(() => ({
    'site.icp': '',
    'site.copyright': 'TODAY & 2017 - 2024 All Rights Reserved.',
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

export const layout: RunTimeLayoutConfig = ({ initialState, loading, setInitialState, }) => {
  return {
    actionsRender: () => [
      // <Question key="doc"/>,
      <SelectLang key="SelectLang"/>,
    ],
    avatarProps: {
      src: initialState?.currentUser?.avatar,
      title: <AvatarName/>,
      render: (_, avatarChildren) => (
          <AvatarDropdown>{avatarChildren}</AvatarDropdown>
      ),
    },
    waterMarkProps: {
      // content: initialState?.currentUser?.name,
    },
    footerRender: () => <Footer/>,
    onPageChange: () => {
      const { location } = history;
      // 如果没有登录，重定向到 login
      if (!initialState?.currentUser && location.pathname !== loginPath) {
        history.push(loginPath);
      }
    },
    bgLayoutImgList: [
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/D2LWSqNny4sAAAAAAAAAAAAAFl94AQBr',
        left: 85,
        bottom: 100,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/C2TWRpJpiC0AAAAAAAAAAAAAFl94AQBr',
        bottom: -68,
        right: -45,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/F6vSTbj8KpYAAAAAAAAAAAAAFl94AQBr',
        bottom: 0,
        left: 0,
        width: '331px',
      },
    ],
    links: [],
    menuHeaderRender: undefined,
    // 自定义 403 页面
    unAccessible: <Forbidden/>,
    // 增加一个 loading 的状态
    childrenRender: (children) => {
      if (loading) return <PageLoading/>;
      return (
          <>
            {children}
            {isDevOrTest && (
                <SettingDrawer
                    disableUrlParams
                    enableDarkTheme
                    settings={initialState?.settings}
                    onSettingChange={(settings) => {
                      setInitialState((preInitialState) => ({
                        ...preInitialState,
                        settings,
                      }));
                    }}
                />
            )}
          </>
      );
    },
    ...initialState?.settings,
  };
};

// https://umijs.org/zh-CN/plugins/plugin-layout
// export const layout: RunTimeLayoutConfig = ({ initialState }) => {
//   return {
//     rightContentRender: () => <RightContent/>,
//     disableContentMargin: false,
//     footerRender: () => <Footer/>,
//     menuHeaderRender: undefined,
//     unAccessible: <Forbidden/>,
//     ...initialState?.settings,
//   }
// }

export const request: RequestConfig = {}
