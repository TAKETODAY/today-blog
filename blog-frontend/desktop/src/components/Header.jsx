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
import { Link } from "react-router-dom";
import { Dropdown, Layout, Menu, message, Modal } from 'antd';
import { ExclamationCircleOutlined, LogoutOutlined } from '@ant-design/icons';
import { connect } from "react-redux";

import { userService } from 'src/services';
import { updateUserSession } from "src/redux/actions";
import { AdminLink, ApiLink, Image, Search } from 'src/components';

const { confirm } = Modal;
const MenuItem = Menu.Item

const { SubMenu } = Menu

const { Header } = Layout;

class NavHeader extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.props.userSession !== nextProps.userSession
  // }

  state = {
    current: 'mail',
    collapsed: false,
    mode: 'horizontal'
  };

  logout = () => {
    userService.logout().then(res => {
      message.info('登出成功!')
      this.props.updateUserSession(null)
    }).catch(resp => {

    })
  }

  showLogoutConfirm = () => {
    confirm({
      title: '安全退出',
      icon: <ExclamationCircleOutlined/>,
      content: '您确认要安全退出系统吗？',
      onOk: () => {
        this.logout()
      },
    })
  }

  renderAdmin = () => {
    return this.props.userSession.blogger &&
      <SubMenu title="管理员">
        <MenuItem key="write" style={{ width: '200px' }}>
          <AdminLink href="/articles/write" target="_blank">写文章</AdminLink>
        </MenuItem>
        <MenuItem key="admin">
          <AdminLink to="/" target="_blank">后台管理</AdminLink>
        </MenuItem>
      </SubMenu>
  }

  renderUserMenu = () => {
    return (
      <Menu style={{ width: '200px', top: '15px', left: '5px' }}>
        <MenuItem>
          <Link to="/user/info" title="个人主页">个人主页 <i className="pull-right glyphicon glyphicon-user"/></Link>
        </MenuItem>
        <MenuItem>
          <Link to="/user/settings" title="设置">设置 <i className="pull-right glyphicon glyphicon-cog"/></Link>
        </MenuItem>
        {this.renderAdmin()}
        <Menu.Divider/>
        <MenuItem>
          <a onClick={this.showLogoutConfirm} title="安全退出">
            安全退出 <LogoutOutlined className=" pull-right"/>
          </a>
        </MenuItem>
      </Menu>
    )
  }


  handleClick = e => {
    this.setState({ current: e.key });
  }

  render() {
    const { userSession, categories, labels } = this.props

    return (<>
      <Header className='navbar'>
        <a href='/' className="navbar-brand" title="首页">
          <img width="69" height="40" alt="Logo" src="/logo.svg" style={{ margin: "-6px" }}/>
        </a>
        <Menu style={{ borderBottom: 'none', marginTop: 4 }}
              onClick={this.handleClick} mode="horizontal"
              className='nav navbar-right' defaultSelectedKeys={['home']}>
          <Menu.Item key="home">
            <Link to='/' title="首页"> <i className="fa fa-home"/> 首页 </Link>
          </Menu.Item>
          <SubMenu key="categories" icon={<i className="fa fa-folder"/>} title="&nbsp;分类">
            <Menu.ItemGroup title="全部分类">
              {categories && categories.map((category, idx) => {
                return (
                  <MenuItem key={`${category.name}-${idx}`}>
                    <Link to={`/categories/${category.name}`} title={category.description}>
                      {category.name}({category.articleCount})
                    </Link>
                  </MenuItem>
                )
              })}
            </Menu.ItemGroup>
          </SubMenu>
          <SubMenu key="tags" icon={<i className="fa fa-tag"/>} title="&nbsp;标签">
            <Menu.ItemGroup title="全部标签">
              {labels && labels.map((tag, idx) => {
                return (
                  <MenuItem key={`${tag.name}-${idx}`}>
                    <Link to={`/tags/${tag.name}`} title={tag.name}>{tag.name}</Link>
                  </MenuItem>
                )
              })}
            </Menu.ItemGroup>
          </SubMenu>
          <Menu.Item key="atom">
            <ApiLink target='_blank' href='/atom.xml' title="订阅"> <i className="fa fa-rss"/> 订阅 </ApiLink>
          </Menu.Item>
          <Menu.Item key="search">
            <Search/>
          </Menu.Item>
          <Menu.Item key="userSession">
            {userSession ?
              <Dropdown overlay={this.renderUserMenu()} placement="bottomRight" trigger='click'>
                <Image original width="30" src={userSession.avatar} className="avatar"/>
              </Dropdown>
              : <Link to='/login' title="登录"><i className="fa fa-sign-in"/> 登录</Link>
            }
          </Menu.Item>

        </Menu>
      </Header>
    </>)
  }
}

export default connect(
  (state) => {
    return {
      userSession: state.user.session,
      windowWidth: state.options.windowWidth,
      ...state.article
    }
  },
  {
    updateUserSession
  }
)(NavHeader)
