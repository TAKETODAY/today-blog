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
import { connect } from "react-redux";
import { Button, message, Modal } from 'antd';
import { withRouter } from "react-router-dom";
import { commentService } from 'src/services';
import { Editor, Image, Login } from 'src/components';
import { updateUserSession } from "src/redux/actions";
import { getForward, isEmpty, isNotEmpty } from 'src/utils';
import { userSessionOptionsMapStateToProps } from "src/redux/action-types";

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
      commentService
        .createComment(commentId ? { commentId, articleId, content } : { articleId, content })
        .then(res => {
          if (res.data) {
            if (res.data.success) {
              this.editor.value("")
              message.success(res.data.message)
            }
            else {
              message.error(res.data.message)
            }
          }
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
          else {
            this.props.history.push("/internal-server-error");
          }
          message.error(err.response.data?.message)
        })
    }
  }

  cancelReply = () => {
    this.props.cancelReply && this.props.cancelReply()
  }

  renderReply(user) {
    return (<>
      <i className='fa fa-reply fa-flip-horizontal'/>
      <div className='comment_user_info' style={{ display: 'inline', marginLeft: '5px' }}>
        <a href={user.site}>
          <Image className='comment_avatar' alt={user.name} src={user.image} title={user.name}
                 style={{ borderRadius: '6px', width: '40px' }}/>
        </a>
        <div className='user_info'>
          <a href={user.site} target='_blank'>
            <span className='user_name'>{user.name}</span>
          </a>
        </div>
        <div className='user_description'>{user.introduce}</div>
      </div>
      <div style={{ float: 'right', marginRight: '-10px' }}>
        <Button onClick={this.cancelReply} type="primary"> 取消评论</Button>
      </div>
    </>)
  }

  render() {
    const { userSession, replying } = this.props
    if (isEmpty(userSession)) {
      return (<>
        <Modal width='300px' open={isNotEmpty(replying) || this.state.loginVisible} onCancel={() => {
          this.loginVisible(false);
          this.cancelReply()
        }} footer={null}>
          <Login forward={getForward()}/>
        </Modal>
        <div className="data_list_title" style={{ border: 'none' }}>
          <i className="fa fa-edit"/> 发表评论
          <p className="textCenter">目前您尚未登录，请<a onClick={() => {
            this.loginVisible(true)
          }}> 登录</a> 后进行评论</p>
        </div>
      </>)
    }

    return (<>
      <div className="data_list_title" style={{ border: 'none' }}>
        {replying
          ? this.renderReply(replying)
          : <><i className="fa fa-edit"/> 发表评论</>
        }
      </div>
      <Editor placeholder={this.props.options['comment.placeholder']}
              setEditor={this.setEditor} options={{ autofocus: false }}/>
      <div className="alignRight">{/* comment_footer */}
        <Button type="primary" onClick={this.createComment}>发表评论</Button>
      </div>
    </>)
  }
}

export default connect(
  userSessionOptionsMapStateToProps, { updateUserSession }
)(withRouter(CommentEdit))
