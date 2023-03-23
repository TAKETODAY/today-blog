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
import moment from 'moment';
import { Tooltip } from 'antd';

export default class InfoRight extends React.Component {

  render() {
    const { userSession } = this.props
    return (
      <div className="shadow-box" style={{ overflow: 'visible' }}>
        <div style={{ margin: '-10px' }}>
          <div className="user_info_item">
            <label>姓名</label>
            <span> {userSession.name}</span>
          </div>
          <div className="user_info_item">
            <label>邮箱</label>
            <span> {userSession.email}</span>
          </div>
          <div className="user_info_item">
            <label>自我介绍</label>
            <span> {userSession.introduce}</span>
          </div>
          <div className="user_info_item">
            <label>个人网站</label>
            <span><a href={userSession.site} target="_blank"> {userSession.site}</a></span>
          </div>
          <div className="user_info_item">
            <label>注册时间</label>
            <Tooltip title={new Date(userSession.id).toLocaleString()}>
              <span style={{ cursor: 'pointer' }}>
                <time> {moment(userSession.id).fromNow()} </time>
              </span>
            </Tooltip>
          </div>
        </div>
      </div>
    )
  }
}

