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
import { LogoutOutlined, SettingOutlined } from '@ant-design/icons';
import { Avatar, Menu, Modal, Spin } from 'antd';
import { history, Link, useModel } from 'umi';
import HeaderDropdown from '../HeaderDropdown';
import styles from './index.less';
import { outLogin } from '@/services/ant-design-pro/login';
import { goToLogin, isNotLoginPage } from "@/utils";

export type GlobalHeaderRightProps = {
  menu?: boolean;
};

const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({ menu }) => {
  const { initialState, setInitialState } = useModel('@@initialState');
  /**
   * 退出登录，并且将当前的 url 保存
   */
  const loginOut = async () => {
    Modal.confirm({
      title: "确定退出登录吗",
      content: (
          <span style={{ color: "red" }}>
          <strong>如果有数据未保存请保存后退出</strong>
        </span>
      ),
      cancelText: '手滑了',
      okText: '确认退出',
      onOk: async () => {
        await outLogin();
        if (isNotLoginPage(history.location)) {
          goToLogin()
        }
        setInitialState({ ...initialState, currentUser: undefined });
      },
    })
  };

  const loading = (
      <span className={`${styles.action} ${styles.account}`}>
      <Spin
          size="small"
          style={{
            marginLeft: 8,
            marginRight: 8,
          }}
      />
    </span>
  );

  if (!initialState) {
    return loading;
  }

  const { currentUser } = initialState;

  if (!currentUser || !currentUser.name) {
    return loading;
  }


  const menuItems = [
    {
      key: 'account',
      icon: <SettingOutlined/>,
      label: <Link to="/system/account">账户设置</Link>,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined/>,
      label: <a href="javascript:void(0)" onClick={loginOut}>退出登录</a>,
    }
  ]

  const menuHeaderDropdown = (
      <Menu className={styles.menu} selectedKeys={[]} items={menuItems}>
      </Menu>
  )

  return (
      <HeaderDropdown overlay={menuHeaderDropdown}>
      <span className={`${styles.action} ${styles.account}`}>
        <Avatar size="small" className={styles.avatar} src={currentUser.image} alt="avatar"/>
        <span className={`${styles.name} anticon`}>{currentUser.name}</span>
      </span>
      </HeaderDropdown>
  );
};

export default AvatarDropdown;
