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

import { Tooltip } from 'antd';
import moment from 'moment';
import React from 'react';
import { isEmpty, scrollTo } from '../../utils';
import { Image } from '../index';

export default class Comment extends React.Component {

  onReply = (user, commentId) => {
    scrollTo('#comment_area')
    this.props.onReply(user, commentId)
  }

  renderReplies(replies, parentUser) {
    if (isEmpty(replies)) {
      return <></>;
    }
    return (<>
      <div className='sub_comment'>
        {
          replies.map((reply, index) => {
            let replyUser = reply.user
            return (
              <div className='sub_comment_item' key={reply.id}>
                <div className='comment_user_info'>
                  <a href={replyUser.site}>
                    <Image
                      alt={replyUser.name}
                      src={replyUser.avatar}
                      title={replyUser.name}
                      className='comment_avatar'/>
                  </a>
                  <div className='user_info'>
                    <a href={replyUser.site} target='_blank'>
                      <span className='user_name'>{replyUser.name}</span>
                    </a>回复
                    <a href={parentUser.site} target='_blank'>
                      <span className='user_name'>{parentUser.name}</span>
                    </a>
                    <Tooltip title={reply.createAt}>
                      <span style={{ cursor: 'pointer' }}>
                        <time> {moment(reply.createAt).fromNow()} </time>
                      </span>
                    </Tooltip>
                  </div>
                  <div className='user_description'> {replyUser.introduce} </div>
                </div>
                <div className='sub_comment_content'>
                  <div className='content' dangerouslySetInnerHTML={{ __html: reply.content }}/>
                  <div className='replyOper'>
                    <a className='replyBtn' title={`回复 ${replyUser.name}`}
                       onClick={() => {
                         this.onReply(replyUser, reply.id)
                       }}>
                      <i className='fa fa-reply'>
                        <span> 回复 </span>
                      </i>
                    </a>
                  </div>
                </div>
                {this.renderReplies(reply.replies, replyUser)}
              </div>
            )
          })
        }
      </div>
    </>)
  }

  render() {
    return (<>
      {this.props.data.map((comment, idx) => {
        const user = comment.user
        return (
          <div key={comment.id} className='comment'>
            <div className='comment_user_info'>
              <a href={user.site}>
                <Image
                  alt={user.name}
                  src={user.avatar}
                  title={user.name}
                  className='comment_avatar'/>
              </a>
              <div className='user_info'>
                <a href={user.site} target='_blank'>
                  <span className='user_name'> {user.name} </span>
                </a>
                <Tooltip title={comment.createAt}>
                  <span style={{ cursor: 'pointer' }}>
                    <time> {moment(comment.createAt).fromNow()} </time>
                  </span>
                </Tooltip>
              </div>
              <div className='user_description'> {user.introduce} </div>
            </div>
            <div className='comment_content'>
              <div className='content' dangerouslySetInnerHTML={{ __html: comment.content }}></div>
              <div className='replyOper'>
                <a className='replyBtn' title={`回复 ${user.name}`}
                   onClick={() => {
                     this.onReply(user, comment.id)
                   }}>
                  <i className='fa fa-reply'>
                    <span> 回复 </span>
                  </i>
                </a>
              </div>
            </div>
            {this.renderReplies(comment.replies, user)}
          </div>
        )
      })
      }
    </>)
  }
}
