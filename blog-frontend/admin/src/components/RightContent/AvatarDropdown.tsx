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
        <Avatar size="small" className={styles.avatar} src={currentUser.avatar} alt="avatar"/>
        <span className={`${styles.name} anticon`}>{currentUser.name}</span>
      </span>
      </HeaderDropdown>
  );
};

export default AvatarDropdown;
