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
                      src={replyUser.image}
                      title={replyUser.name}
                      className='comment_avatar' />
                  </a>
                  <div className='user_info'>
                    <a href={replyUser.site} target='_blank'>
                      <span className='user_name'>{replyUser.name}</span>
                    </a>回复
						        <a href={parentUser.site} target='_blank'>
                      <span className='user_name'>{parentUser.name}</span>
                    </a>
                    <Tooltip title={new Date(reply.id).toLocaleString()}>
                      <span style={{ cursor: 'pointer' }}>
                        <time> {moment(reply.id).fromNow()} </time>
                      </span>
                    </Tooltip>
                  </div>
                  <div className='user_description'> {replyUser.introduce} </div>
                </div>
                <div className='sub_comment_content'>
                  <div className='content' dangerouslySetInnerHTML={ { __html: reply.content } }/>
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
                  src={user.image}
                  title={user.name}
                  className='comment_avatar' />
              </a>
              <div className='user_info'>
                <a href={user.site} target='_blank'>
                  <span className='user_name'> {user.name} </span>
                </a>
                <Tooltip title={new Date(comment.id).toLocaleString()}>
                  <span style={{ cursor: 'pointer' }}>
                    <time> {moment(comment.id).fromNow()} </time>
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
