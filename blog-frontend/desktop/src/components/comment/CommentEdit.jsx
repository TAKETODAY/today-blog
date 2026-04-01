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

import React, { useState } from 'react';
import { connect } from "react-redux";
import { Button, Form, Input, message } from 'antd';
import { GlobalOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';

import { withRouter } from "react-router-dom";
import { commentService } from 'src/services';
import { Editor, Image } from 'src/components';
import { updateUserSession } from "src/redux/actions";
import { userSessionOptionsMapStateToProps } from "src/redux/action-types";
import { getGravatarURL, getStorage, isEmpty, saveStorage } from "core";


const CommentEdit = props => {
  const [editor, setEditor] = useState(null)
  const [form] = Form.useForm();

  const { userSession, replying, options } = props

  const cancelReply = () => {
    props.cancelReply && props.cancelReply()
  }

  const createComment = commenter => {
    saveStorage("comment-metadata", commenter)

    const content = editor.markdown()
    if (isEmpty(content)) {
      return message.warning("请输入评论内容")
    }
    else {
      const { commentId, articleId } = props
      return commentService.createComment({ commentId, articleId, content, ...commenter })
          .then(_ => {
            editor.value("")
            return message.success("评论成功")
          })
          .catch(err => {
            return message.error(err.response.data?.message)
          })
    }
  }


  function renderReply(comment) {
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
        <Button onClick={cancelReply} type="primary"> 取消评论</Button>
      </div>
    </>)
  }


  const commenterInfo = () => {
    if (userSession) {
      return {
        email: userSession?.email,
        commenter: userSession?.name,
        commenterSite: userSession?.site
      }
    }
    return { ...getStorage("comment-metadata") }
  }

  //const commenter = commenterInfo()

  return (<>
    <div className="data-list-title" style={{ border: 'none' }}>
      {replying
          ? renderReply(replying)
          : <><i className="fa fa-edit"/> 发表评论</>
      }
    </div>

    <Editor placeholder={options['comment.placeholder']}
            setEditor={setEditor} options={{ autofocus: false }}/>

    <div className="comment-edit-footer">
      <Form layout="inline" form={form} initialValues={commenterInfo()} onFinish={createComment}>
        <Form.Item name="email" rules={[{ required: true, message: '请输入邮箱' }]}>
          <Input prefix={<MailOutlined/>} placeholder="邮箱"/>
        </Form.Item>
        <Form.Item name="commenter" rules={[{ required: true, message: '请输入昵称' }]}>
          <Input prefix={<UserOutlined/>} placeholder="昵称"/>
        </Form.Item>
        <Form.Item name="commenterSite">
          <Input prefix={<GlobalOutlined/>} placeholder="个人网站"/>
        </Form.Item>
        <Form.Item shouldUpdate>
          {() => (
              <Button type="primary" htmlType="submit">提交评论</Button>
          )}
        </Form.Item>
      </Form>
    </div>

  </>)

}


export default connect(
    userSessionOptionsMapStateToProps, { updateUserSession }
)(withRouter(CommentEdit))
