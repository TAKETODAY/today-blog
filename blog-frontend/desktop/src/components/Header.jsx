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
import { Dropdown, Layout, Menu, message, Modal, Select } from 'antd';
import 'antd/es/dropdown/style';
import 'antd/es/layout/style';
import 'antd/es/menu/style';
import 'antd/es/message/style';
import 'antd/es/modal/style';
import 'antd/es/select/style';
import { ExclamationCircleOutlined, FolderFilled, HomeFilled, LoginOutlined, LogoutOutlined, TagFilled } from '@ant-design/icons';
import RssIcon from './RssIcon';
import { useDispatch, useSelector } from "react-redux";

import { userService } from 'src/services';
import { updateUserSession } from "src/redux/actions";
import AdminLink from './AdminLink';
import ApiLink from './ApiLink';
import Image from './Image';
import Search from './Search';
import { translate } from 'core';
import { getCurrentTimeZone, getVisitorLocale, getVisitorTimeZone, setVisitorPreference } from '../../../common/visitor-preferences';
import { localeOptions } from '../../../common/site-settings';
import { useDisplayPreferences } from './hooks/useDisplayPreferences';


const { Header } = Layout;

export default function NavHeader() {
  useDisplayPreferences();
  const dispatch = useDispatch()
  const userSession = useSelector(state => state.user.session)
  const categories = useSelector(state => state.article.categories)
  const labels = useSelector(state => state.article.labels)
  const timeZones = [...new Set([
    'UTC', 'Asia/Shanghai', 'America/New_York', 'Europe/London', getCurrentTimeZone(),
    ...(Intl.supportedValuesOf?.('timeZone') || []),
  ])];

  async function logout() {
    try {
      await userService.logout()
      dispatch(updateUserSession(null))
      message.success(translate('已退出登录', 'Signed out'))
    }
    catch (error) {
      message.error(translate('退出失败，请稍后重试', 'Could not sign out. Please try again.'))
      throw error
    }
  }

  function showLogoutConfirm() {
    Modal.confirm({
      title: translate('安全退出', 'Sign out'),
      icon: <ExclamationCircleOutlined/>,
      content: translate('您确认要安全退出系统吗？', 'Do you want to sign out?'),
      okText: translate('确认', 'Confirm'),
      cancelText: translate('取消', 'Cancel'),
      onOk: logout,
    })
  }

  function renderAdmin() {
    return userSession.blogger ? [
      {
        key: 'write',
        style: { width: '200px' },
        label: <AdminLink href="/articles/write" target="_blank">{translate('写文章', 'Write')}</AdminLink>,
      },
      {
        key: 'admin',
        label: <AdminLink to="/" target="_blank">{translate('后台管理', 'Admin')}</AdminLink>,
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
          label: <span title={translate('安全退出', 'Sign out')}>{translate('安全退出', 'Sign out')} <LogoutOutlined/></span>,
        },
      ],
    }
  }

  const items = [
    {
      key: 'home',
      label: <Link to='/' title={translate('首页', 'Home')}><HomeFilled aria-hidden="true"/> {translate('首页', 'Home')}</Link>,
    },
    {
      key: 'categories',
      icon: <FolderFilled aria-hidden="true"/>,
      label: translate('分类', 'Categories'),
      children: [{
        type: 'group',
        key: 'all-categories',
        label: translate('全部分类', 'All categories'),
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
      label: translate('标签', 'Tags'),
      children: [{
        type: 'group',
        key: 'all-tags',
        label: translate('全部标签', 'All tags'),
        children: (labels || []).map((tag, idx) => ({
          key: `${tag.name}-${idx}`,
          label: <Link to={`/tags/${tag.name}`} title={tag.name}>{tag.name}</Link>,
        })),
      }],
    },
    {
      key: 'atom',
      label: <ApiLink target='_blank' href='/feed.atom' title={translate('订阅', 'Subscribe')}><RssIcon/> {translate('订阅', 'Subscribe')}</ApiLink>,
    },
    { key: 'search', label: <Search/> },
    {
      key: 'locale',
      label: <Select
        size="small"
        aria-label={translate('显示语言', 'Display language')}
        value={getVisitorLocale() || 'browser'}
        onChange={value => setVisitorPreference('locale', value === 'browser' ? null : value)}
        options={[
          { value: 'browser', label: translate('跟随浏览器', 'Browser language') },
          { value: 'site', label: translate('站点默认语言', 'Site language') },
          ...localeOptions,
        ]}
      />,
    },
    {
      key: 'timezone',
      label: <Select
        size="small"
        aria-label={translate('显示时区', 'Display time zone')}
        showSearch
        optionFilterProp="label"
        value={getVisitorTimeZone() || 'site'}
        onChange={value => setVisitorPreference('timeZone', value === 'site' ? null : value)}
        options={[
          { value: 'site', label: translate('站点默认时区', 'Site time zone') },
          ...timeZones.map(value => ({ value, label: value })),
        ]}
      />,
    },
    {
      key: 'userSession',
      label: userSession ?
        <Dropdown menu={renderUserMenu()} placement="bottomLeft" trigger={['click']}>
          <span>
            <Image original width="30" src={userSession.avatar} className="avatar"/>
          </span>
        </Dropdown>
        : <Link to='/login' title={translate('登录', 'Sign in')}><LoginOutlined aria-hidden="true"/> {translate('登录', 'Sign in')}</Link>,
    },
  ]

  return (<>
    <Header className='navbar'>
      <div className="navbar-brand">
        <a href='/' title={translate('首页', 'Home')}>
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
