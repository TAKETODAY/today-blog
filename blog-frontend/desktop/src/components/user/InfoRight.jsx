import React from 'react';
import moment from 'moment';
import { Tooltip } from 'antd';

export default class InfoRight extends React.Component {

  render() {
    const { userSession } = this.props
    return (
      <div className="data_list" style={{ overflow: 'visible' }}>
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

