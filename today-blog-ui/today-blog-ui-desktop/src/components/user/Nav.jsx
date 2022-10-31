import React from 'react';
import { Link } from 'react-router-dom';

export default class UserNav extends React.Component {
  
  render() {
    return (
      <div className="back_info_nav">
        <div className="info_nav">
          <div className="info_nav_btn">
            <Link to="/user/info" title="个人信息">
              <span id="userInfo" className="active">信息</span>
            </Link>
          </div>
          <div className="info_nav_btn">
            <Link to="/user/settings" title="设置">
              <span id="userSetting">设置</span>
            </Link>
          </div>
        </div>
      </div>
    )
  }
}