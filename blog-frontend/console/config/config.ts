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

// https://umijs.org/config/
import { defineConfig } from '@umijs/max';
import defaultSettings from './defaultSettings';
import proxy from './proxy';
import routes from './routes';

const CompressionPlugin = require('compression-webpack-plugin')

const { REACT_APP_ENV } = process.env;
const production = process.env.NODE_ENV === 'production'

export default defineConfig({
  // Umi 3 的 Pro preset 曾注入这些变量；Umi 4 需要显式定义浏览器替换值。
  define: {
    REACT_APP_ENV: REACT_APP_ENV || false,
    ANT_DESIGN_PRO_ONLY_DO_NOT_USE_IN_YOUR_PRODUCTION: false,
  },
  hash: true, // 配置是否让生成的文件包含 hash 后缀，通常用于增量发布和避免浏览器加载缓存。
  antd: {},

  dva: {},
  model: {},
  initialState: { loading: '@/components/InitialStateLoading' },
  access: { strictMode: true },
  request: {},
  title: 'TODAY BLOG',
  history: {
    type: 'hash'
  },
  // base: '/blog-admin/',
  // publicPath: '/blog-admin/',
  publicPath: production ? '/blog-admin/' : '/',
  layout: {
    // https://umijs.org/zh-CN/plugins/plugin-layout
    locale: true,
    siderWidth: 208,
    ...defaultSettings,
  },
  // https://umijs.org/zh-CN/plugins/plugin-locale
  locale: {
    // default zh-CN
    default: 'zh-CN',
    antd: true,
    // default true, when it is true, will use `navigator.language` overwrite default
    baseNavigator: true,
  },
  targets: {
    chrome: 100,
  },
  // umi routes: https://umijs.org/docs/routing
  routes,
  // Theme for antd: https://ant.design/docs/react/customize-theme-cn
  theme: {
    'primary-color': defaultSettings.primaryColor,
  },
  ignoreMomentLocale: true,
  proxy: proxy[REACT_APP_ENV || 'dev'],
  manifest: {
    basePath: '/',
  },
  mfsu: false,
  chainWebpack: function (config) {
    if (production) {

      //gzip压缩
      config.plugin('compression-webpack-plugin').use(CompressionPlugin, [
        {
          test: /\.js$|\.html$|\.css$/, //匹配文件名
          threshold: 10240, //对超过10k的数据压缩
          deleteOriginalAssets: false, //不删除源文件
        },
      ]);

      //过滤掉momnet的那些不使用的国际化文件
      config.plugin("replace").use(require("webpack").ContextReplacementPlugin).tap(() => {
        return [/moment[/\\]locale$/, /zh-cn/];
      })
    }

  }
});
