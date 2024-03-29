import { Space, Tag } from 'antd';
import React from 'react';
import { SelectLang, useModel } from 'umi';
import Avatar from './AvatarDropdown';
import styles from './index.less';

const ENVTagColor = {
  dev: 'orange',
  test: 'green',
  pre: '#87d068',
};

const GlobalHeaderRight: React.FC = () => {
  const { initialState } = useModel('@@initialState');

  if (!initialState || !initialState.settings) {
    return null;
  }

  const { navTheme, layout } = initialState.settings;
  let className = styles.right;

  if ((navTheme === 'dark' && layout === 'top') || layout === 'mix') {
    className = `${styles.right}  ${styles.dark}`;
  }
  return (
      <Space className={className}>
        <Avatar menu/>
        {REACT_APP_ENV && (
            <span>
          <Tag color={ENVTagColor[REACT_APP_ENV]}>{REACT_APP_ENV}</Tag>
        </span>
        )}
        <SelectLang className={styles.action}/>
      </Space>
  );
};
export default GlobalHeaderRight;
