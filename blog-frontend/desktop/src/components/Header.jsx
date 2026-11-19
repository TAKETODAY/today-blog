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
import { translate } from '../locales';
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
      message.success(translate('navigation.signedOut'))
    }
    catch (error) {
      message.error(translate('navigation.couldNotSignOutPlease'))
      throw error
    }
  }

  function showLogoutConfirm() {
    Modal.confirm({
      title: translate('navigation.signOut'),
      icon: <ExclamationCircleOutlined/>,
      content: translate('navigation.doYouWantToSign'),
      okText: translate('navigation.confirm'),
      cancelText: translate('navigation.cancel'),
      onOk: logout,
    })
  }

  function renderAdmin() {
    return userSession.blogger ? [
      {
        key: 'write',
        style: { width: '200px' },
        label: <AdminLink href="/articles/write" target="_blank">{translate('navigation.write')}</AdminLink>,
      },
      {
        key: 'admin',
        label: <AdminLink to="/" target="_blank">{translate('navigation.admin')}</AdminLink>,
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
          label: <span title={translate('navigation.signOut')}>{translate('navigation.signOut')} <LogoutOutlined/></span>,
        },
      ],
    }
  }

  const items = [
    {
      key: 'home',
      label: <Link to='/' title={translate('navigation.home')}><HomeFilled aria-hidden="true"/> {translate('navigation.home')}</Link>,
    },
    {
      key: 'categories',
      icon: <FolderFilled aria-hidden="true"/>,
      label: translate('navigation.categories'),
      children: [{
        type: 'group',
        key: 'all-categories',
        label: translate('navigation.allCategories'),
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
      label: translate('navigation.tags'),
      children: [{
        type: 'group',
        key: 'all-tags',
        label: translate('navigation.allTags'),
        children: (labels || []).map((tag, idx) => ({
          key: `${tag.name}-${idx}`,
          label: <Link to={`/tags/${tag.name}`} title={tag.name}>{tag.name}</Link>,
        })),
      }],
    },
    {
      key: 'atom',
      label: <ApiLink target='_blank' href='/feed.atom' title={translate('navigation.subscribe')}><RssIcon/> {translate('navigation.subscribe')}</ApiLink>,
    },
    { key: 'search', label: <Search/> },
    {
      key: 'locale',
      label: <Select
        size="small"
        aria-label={translate('navigation.displayLanguage')}
        value={getVisitorLocale() || 'browser'}
        onChange={value => setVisitorPreference('locale', value === 'browser' ? null : value)}
        options={[
          { value: 'browser', label: translate('navigation.browserLanguage') },
          { value: 'site', label: translate('navigation.siteLanguage') },
          ...localeOptions,
        ]}
      />,
    },
    {
      key: 'timezone',
      label: <Select
        size="small"
        aria-label={translate('navigation.displayTimeZone')}
        showSearch
        optionFilterProp="label"
        value={getVisitorTimeZone() || 'site'}
        onChange={value => setVisitorPreference('timeZone', value === 'site' ? null : value)}
        options={[
          { value: 'site', label: translate('navigation.siteTimeZone') },
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
        : <Link to='/login' title={translate('navigation.signIn')}><LoginOutlined aria-hidden="true"/> {translate('navigation.signIn')}</Link>,
    },
  ]

  return (<>
    <Header className='navbar'>
      <div className="navbar-brand">
        <a href='/' title={translate('navigation.home')}>
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
