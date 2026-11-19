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

import React from 'react';
import head from '../assets/images/404/head.png';
import { Button } from "antd";
import 'antd/es/button/style';
import { translate } from 'core';

const errors = {
  '400': ['错误请求', 'Bad request', '请检查请求后重试。', 'Check your request and try again.'],
  '403': ['禁止访问', 'Access forbidden', '您可能没有访问权限。', 'You may not have permission to access this page.'],
  '404': ['页面未找到', 'Page not found', '您请求的页面不存在。', 'The requested page could not be found.'],
  '405': ['不支持此请求方式', 'Method not allowed', '此页面不支持您使用的请求方式。', 'This page does not support the requested method.'],
  '500': ['服务器错误', 'Internal server error', '服务器出错，请稍后重试。', 'Something went wrong. Please try again later.'],
}

export default props => {
  const status = Object.hasOwn(errors, props.status) ? props.status : '500';
  const [zhTitle, enTitle, zhDetail, enDetail] = errors[status];
  const title = `${status} ${translate(zhTitle, enTitle)}`;
  document.title = title;
  return (<>
    <div className="container" style={{ marginTop: '100px' }} align="center">
      <div className="img">
        <img src={head} alt=""/>
      </div>
      <h1 style={{ fontSize: '48px' }}>{title}</h1>
      <div id="msg" style={{ fontSize: '15px', marginBottom: 20 }}>{translate(zhDetail, enDetail)}</div>

      <Button onClick={() => window.location.replace('/')} style={{ marginRight: 10 }}>{translate('首页', 'Home')}</Button>

      <Button type="primary" onClick={() => window.history.back()}>{translate('返回', 'Back')}</Button>
    </div>
  </>)
}
