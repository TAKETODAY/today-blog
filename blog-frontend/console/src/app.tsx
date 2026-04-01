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

import type { RequestConfig, RunTimeLayoutConfig } from 'umi';
import { history } from 'umi';
import type { Settings as LayoutSettings } from '@ant-design/pro-layout';
import { PageLoading } from '@ant-design/pro-layout';

import Footer from '@/components/Footer';
import RightContent from '@/components/RightContent';
import { extractData, getCacheable } from "@/utils";
import { currentUser as queryCurrentUser } from './services/ant-design-pro/api';
import Forbidden from "@/pages/403";

/** 获取用户信息比较慢的时候会展示一个 loading */
export const initialStateConfig = {
  loading: <PageLoading/>,
}

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

// https://umijs.org/zh-CN/plugins/plugin-layout
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    rightContentRender: () => <RightContent/>,
    disableContentMargin: false,
    footerRender: () => <Footer/>,
    menuHeaderRender: undefined,
    unAccessible: <Forbidden/>,
    ...initialState?.settings,
  }
}

export const request: RequestConfig = {}
