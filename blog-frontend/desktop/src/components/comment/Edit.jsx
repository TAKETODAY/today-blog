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

import React from 'react';
import { connect } from "react-redux";
import { Button, Form, Input, message } from 'antd';
import { GlobalOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';

import { withRouter } from "react-router-dom";
import { commentService } from 'src/services';
import { Editor, Image } from 'src/components';
import { updateUserSession } from "src/redux/actions";
import { isEmpty } from 'src/utils';
import { userSessionOptionsMapStateToProps } from "src/redux/action-types";
import { getGravatarURL } from "../../utils";

class CommentEdit extends React.Component {

  state = {
    loginVisible: false
  }

  loginVisible = (loginVisible) => {
    this.setState({ loginVisible })
  }

  editor = null

  setEditor = (editor) => {
    this.editor = editor
  }

  createComment = () => {
    const content = this.editor.markdown()
    if (isEmpty(content)) {
      message.warning("请输入评论内容")
    }
    else {
      const { commentId, articleId } = this.props
      commentService.createComment(commentId ? { commentId, articleId, content } : { articleId, content })
        .then(_ => {
          this.editor.value("")
          return message.success("评论成功")
        })
        .catch(err => {
          // console.log(JSON.stringify(err))
          const { status } = err.response || { status: 0 }
          if (status === 401) {
            message.error(err.response.data.message)
            this.props.updateUserSession(null)
            this.loginVisible(true)
          }
          else if (status === 403) {
            message.error(err.response.data.message)
            this.props.history.push("/access-forbidden");
          }
          return message.error(err.response.data?.message)
        })
    }
  }

  cancelReply = () => {
    this.props.cancelReply && this.props.cancelReply()
  }

  renderReply(comment) {
    return (<>
      <i className='fa fa-reply fa-flip-horizontal'/>
      <div className='comment_user_info' style={{ display: 'inline', marginLeft: '5px' }}>
        <a href={comment.commenterSite}>
          <Image className='comment_avatar' alt={comment.commenter}
                 src={getGravatarURL(comment.email)} title={comment.commenter}
                 style={{ borderRadius: '6px', width: '40px' }}/>
        </a>
        <div className='user_info'>
          <a href={comment.commenterSite} target='_blank'>
            <span className='user_name'>{comment.commenter}</span>
          </a>
        </div>
        <div className='user_description'>{comment.commenterDesc || comment.user.introduce}</div>
      </div>
      <div style={{ float: 'right', marginRight: '-10px' }}>
        <Button onClick={this.cancelReply} type="primary"> 取消评论</Button>
      </div>
    </>)
  }

  render() {
    const { userSession, replying } = this.props

    return (<>
      <div className="data-list-title" style={{ border: 'none' }}>
        {replying
          ? this.renderReply(replying)
          : <><i className="fa fa-edit"/> 发表评论</>
        }
      </div>
      <Editor placeholder={this.props.options['comment.placeholder']}
              setEditor={this.setEditor} options={{ autofocus: false }}/>
      <div className="alignRight">{/* comment_footer */}

        <Form layout="inline">
          <Form.Item name="email" rules={[{ required: true, message: '请输入邮箱' }]}>
            <Input prefix={<MailOutlined/>} placeholder="邮箱" defaultValue={userSession?.email}/>
          </Form.Item>
          <Form.Item name="name" rules={[{ required: true, message: '请输入昵称' }]}>
            <Input prefix={<UserOutlined/>} placeholder="昵称" defaultValue={userSession?.name}/>
          </Form.Item>
          <Form.Item name="site">
            <Input prefix={<GlobalOutlined/>} placeholder="个人网站" defaultValue={userSession?.site}/>
          </Form.Item>
          <Form.Item shouldUpdate>
            {() => (
              <Button type="primary" htmlType="submit" onClick={this.createComment}>提交评论</Button>
            )}
          </Form.Item>
        </Form>

      </div>
    </>)
  }
}

export default connect(
  userSessionOptionsMapStateToProps, { updateUserSession }
)(withRouter(CommentEdit))
