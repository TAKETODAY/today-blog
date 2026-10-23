/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import React from 'react';
import { Link } from "react-router-dom";
import { Dropdown, Layout, Menu, message, Modal } from 'antd';
import 'antd/es/dropdown/style';
import 'antd/es/layout/style';
import 'antd/es/menu/style';
import 'antd/es/message/style';
import 'antd/es/modal/style';
import { ExclamationCircleOutlined, FolderFilled, HomeFilled, LoginOutlined, LogoutOutlined, TagFilled } from '@ant-design/icons';
import RssIcon from './RssIcon';
import { useDispatch, useSelector } from "react-redux";

import { userService } from 'src/services';
import { updateUserSession } from "src/redux/actions";
import AdminLink from './AdminLink';
import ApiLink from './ApiLink';
import Image from './Image';
import Search from './Search';


const { Header } = Layout;

export default function NavHeader() {
  const dispatch = useDispatch()
  const userSession = useSelector(state => state.user.session)
  const categories = useSelector(state => state.article.categories)
  const labels = useSelector(state => state.article.labels)

  async function logout() {
    try {
      await userService.logout()
      dispatch(updateUserSession(null))
      message.success('登出成功!')
    }
    catch (error) {
      message.error('退出失败，请稍后重试')
      throw error
    }
  }

  function showLogoutConfirm() {
    Modal.confirm({
      title: '安全退出',
      icon: <ExclamationCircleOutlined/>,
      content: '您确认要安全退出系统吗？',
      onOk: logout,
    })
  }

  function renderAdmin() {
    return userSession.blogger ? [
      {
        key: 'write',
        style: { width: '200px' },
        label: <AdminLink href="/articles/write" target="_blank">写文章</AdminLink>,
      },
      {
        key: 'admin',
        label: <AdminLink to="/" target="_blank">后台管理</AdminLink>,
      },
    ] : []
  }

  function renderUserMenu() {
    return {
      style: { width: '200px', top: '15px', left: '5px' },
      items: [
        ...renderAdmin(),
        { type: 'divider', key: 'divider' },
        {
          key: 'logout',
          onClick: showLogoutConfirm,
          label: <span title="安全退出">安全退出 <LogoutOutlined/></span>,
        },
      ],
    }
  }

  const items = [
    {
      key: 'home',
      label: <Link to='/' title="首页"><HomeFilled aria-hidden="true"/> 首页</Link>,
    },
    {
      key: 'categories',
      icon: <FolderFilled aria-hidden="true"/>,
      label: '分类',
      children: [{
        type: 'group',
        key: 'all-categories',
        label: '全部分类',
        children: (categories || []).map((category, idx) => ({
          key: `${category.name}-${idx}`,
          label: <Link to={`/categories/${category.name}`} title={category.description}>
            {category.name}({category.articleCount})
          </Link>,
        })),
      }],
    },
    {
      key: 'tags',
      icon: <TagFilled aria-hidden="true"/>,
      label: '标签',
      children: [{
        type: 'group',
        key: 'all-tags',
        label: '全部标签',
        children: (labels || []).map((tag, idx) => ({
          key: `${tag.name}-${idx}`,
          label: <Link to={`/tags/${tag.name}`} title={tag.name}>{tag.name}</Link>,
        })),
      }],
    },
    {
      key: 'atom',
      label: <ApiLink target='_blank' href='/feed.atom' title="订阅"><RssIcon/> 订阅</ApiLink>,
    },
    { key: 'search', label: <Search/> },
    {
      key: 'userSession',
      label: userSession ?
        <Dropdown menu={renderUserMenu()} placement="bottomLeft" trigger={['click']}>
          <span>
            <Image original width="30" src={userSession.avatar} className="avatar"/>
          </span>
        </Dropdown>
        : <Link to='/login' title="登录"><LoginOutlined aria-hidden="true"/> 登录</Link>,
    },
  ]

  return (<>
    <Header className='navbar'>
      <div className="navbar-brand">
        <a href='/' title="首页">
          <img width="69" height="40" alt="Logo" src="/logo.svg" style={{ margin: "-6px" }}/>
        </a>
      </div>
      <div className="navbar-right">
        <Menu style={{ borderBottom: 'none', marginTop: 4 }}
              mode="horizontal" defaultSelectedKeys={['home']} items={items}/>
      </div>
    </Header>
  </>)
}
