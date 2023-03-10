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
import { Link } from 'react-router-dom';
import { InfoRight, UserDescription } from '../components';
import { Affix, Anchor, Col, Row } from 'antd'

const AnchorLink = Anchor.Link;

export default class UserLayout extends React.Component {

  componentDidMount() {
  }

  render() {
    const { userSession } = this.props
    if (userSession === null) {
      this.props.history.push('/login')
      return null
    }
    if (!userSession) {
      return <></>
    }
    return (<>
      <div className="container" style={{ marginTop: '70px' }}>
        <Row>
          <Col xs={24} md={18} lg={24}>
            {userSession.defaultPassword &&
              <div className="alert alert-info" role="alert">提示：当前使用默认密码:'https://taketoday.cn' 请尽快
                <Link to="/user/settings#password" className="alert-link">修改密码</Link>
              </div>
            }
            <UserDescription userSession={userSession}/>
            {/* <UserNav /> */}
          </Col>
          <Col xs={24} md={18} lg={18}>
            {this.props.children}
          </Col>
          {/*侧边栏*/}
          <Col xs={24} md={6} style={{ paddingLeft: 15, paddingRight: 15 }}>
            <Affix offsetTop={80}>
              <InfoRight userSession={userSession}/>
              <Anchor offsetTop={80} className="list-group">
                <AnchorLink href="settings#info" title="基本信息修改"/>
                <AnchorLink href="settings#email" title="邮箱修改"/>
                <AnchorLink href="settings#password" title="密码修改"/>
                <AnchorLink href="settings#avatar" title="头像修改"/>
                <AnchorLink href="settings#background" title="背景修改"/>
              </Anchor>
            </Affix>

          </Col>
        </Row>
      </div>
    </>)
  }
}
