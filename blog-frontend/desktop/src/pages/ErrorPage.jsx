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

const errors = {
  '400': { msg: '400 Bad Request !', desc: '错误请求 !' },
  '403': { msg: '403 Access Forbidden !', desc: '服务器拒绝处理您的请求！您可能没有访问此操作的权限 !' },
  '404': { msg: '404 NOT Found !', desc: '您请求的页面,我找遍了整个服务器都没找到 !' },
  '405': { msg: '405 Method Not Allowed !', desc: '您请求的方式不正确 !' },
  '500': { msg: '500 Internal Server Error !', desc: '服务器出错啦 !' },
}

export default props => {
  let error = errors[`${props.status}`]
  if (!error) {
    error = errors['500']
  }
  document.title = error.msg
  return (<>
    <div className="container" style={{ marginTop: '100px' }} align="center">
      <div className="img">
        <img src={head} alt={error.msg}/>
      </div>
      <h1 style={{ fontSize: '48px' }}>{error.msg}</h1>
      <div id="msg" style={{ fontSize: '15px', marginBottom: 20 }}>{error.desc}</div>

      <Button onClick={() => window.location.replace('/')} style={{ marginRight: 10 }}>首页</Button>

      <Button type="primary" onClick={() => window.history.back()}>返回</Button>
    </div>
  </>)
}


