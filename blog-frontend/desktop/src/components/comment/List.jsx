/*
 * Copyright 2017 - 2024 the original author or authors.
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

import { Tooltip } from 'antd';
import React from 'react';

import { format, fromNow, getGravatarURL, isEmpty, scrollTo } from '../../utils';
import { Image } from '../index';


export default class CommentList extends React.Component {

  onReply = (comment) => {
    scrollTo('#comment_area')
    this.props.onReply(comment)
  }

  renderReplies(replies, parent) {
    if (isEmpty(replies)) {
      return <></>;
    }
    return (<>
      <div className='sub_comment'>
        {
          replies.map((reply, index) => {
            return (
              <div className='sub_comment_item' key={reply.id}>
                <div className='comment_user_info'>
                  <a href={reply.commenterSite}>
                    <Image alt={reply.commenter} title={reply.commenter} className='comment_avatar'
                           src={getGravatarURL(reply.email)}/>
                  </a>
                  <div className='user_info'>
                    <a href={reply.commenterSite} target='_blank'>
                      <span className='user_name'>{reply.commenter}</span>
                    </a>回复
                    <a href={reply.commenterSite} target='_blank'>
                      <span className='user_name'>{parent.commenter}</span>
                    </a>
                    <Tooltip title={format(reply.createAt)}>
                      <span style={{ cursor: 'pointer' }}>
                        <time> {fromNow(reply.createAt)} </time>
                      </span>
                    </Tooltip>
                  </div>
                  <div className='user_description'> {reply.commenterDesc || reply.user?.introduce} </div>
                </div>
                <div className='sub_comment_content'>
                  <div className='content' dangerouslySetInnerHTML={{ __html: reply.content }}/>
                  <div className='replyOper'>
                    <a className='replyBtn' title={`回复 ${reply.commenter}`}
                       onClick={() => {
                         this.onReply(reply)
                       }}>
                      <i className='fa fa-reply'>
                        <span> 回复 </span>
                      </i>
                    </a>
                  </div>
                </div>
                {this.renderReplies(reply.replies, reply)}
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
        return (
          <div key={comment.id} className='comment'>
            <div className='comment_user_info'>
              <a href={comment.commenterSite}>
                <Image alt={comment.commenter}
                       src={getGravatarURL(comment.email)}
                       title={comment.commenter}
                       className='comment_avatar'/>
              </a>
              <div className='user_info'>
                <a href={comment.commenterSite} target='_blank'>
                  <span className='user_name'> {comment.commenter} </span>
                </a>
                <Tooltip title={format(comment.createAt)}>
                  <span style={{ cursor: 'pointer' }}>
                    <time> {fromNow(comment.createAt)} </time>
                  </span>
                </Tooltip>
              </div>
              <div className='user_description'> {comment.commenterDesc || comment.user?.introduce} </div>
            </div>
            <div className='comment_content'>
              <div className='content' dangerouslySetInnerHTML={{ __html: comment.content }}></div>
              <div className='replyOper'>
                <a className='replyBtn' title={`回复 ${comment.commenter}`}
                   onClick={() => this.onReply(comment)}>
                  <i className='fa fa-reply'>
                    <span> 回复 </span>
                  </i>
                </a>
              </div>
            </div>
            {this.renderReplies(comment.replies, comment)}
          </div>
        )
      })
      }
    </>)
  }
}
