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

import React, { Suspense, useState } from 'react';
import { connect } from "react-redux";
import { Button, Form, Input, message } from 'antd';
import 'antd/es/button/style';
import 'antd/es/form/style';
import 'antd/es/input/style';
import 'antd/es/message/style';
import { EditOutlined, GlobalOutlined, MailOutlined, RollbackOutlined, UserOutlined } from '@ant-design/icons';

import { commentService } from 'src/services';
import Image from '../Image';
import { updateUserSession } from "src/redux/actions";
import { userSessionOptionsMapStateToProps } from "src/redux/action-types";
import { getGravatarURL, getStorage, isEmpty, saveStorage, translate } from "core";
import './CommentEdit.css';
import { useDisplayPreferences } from '../hooks/useDisplayPreferences';


const Editor = React.lazy(() => import('../Editor'));

const CommentEdit = props => {
  useDisplayPreferences();
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
      return message.warning(translate('请输入评论内容', 'Enter your comment.'))
    }
    else {
      const { commentId, articleId } = props
      return commentService.createComment({ commentId, articleId, content, ...commenter })
          .then(_ => {
            editor.value("")
            return message.success(translate('评论成功', 'Comment posted.'))
          })
          .catch(err => message.error(err.message || translate('评论提交失败', 'Could not post the comment.')))
    }
  }


  function renderReply(comment) {
    return (<>
      <RollbackOutlined aria-hidden="true" style={{ transform: 'scaleX(-1)' }}/>
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
        <Button onClick={cancelReply} type="primary">{translate('取消回复', 'Cancel reply')}</Button>
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
          : <><EditOutlined aria-hidden="true"/> {translate('发表评论', 'Leave a comment')}</>
      }
    </div>

    <Suspense fallback={<div>{translate('编辑器加载中…', 'Loading editor…')}</div>}>
      <Editor placeholder={options['comment.placeholder']}
              setEditor={setEditor} options={{ autofocus: false }}/>
    </Suspense>

    <div className="comment-edit-footer">
      <Form layout="inline" form={form} initialValues={commenterInfo()} onFinish={createComment}>
        <Form.Item name="email" rules={[{ required: true, message: translate('请输入邮箱', 'Enter your email address.') }]}>
          <Input prefix={<MailOutlined/>} placeholder={translate('邮箱', 'Email')}/>
        </Form.Item>
        <Form.Item name="commenter" rules={[{ required: true, message: translate('请输入昵称', 'Enter a display name.') }]}>
          <Input prefix={<UserOutlined/>} placeholder={translate('昵称', 'Display name')}/>
        </Form.Item>
        <Form.Item name="commenterSite">
          <Input prefix={<GlobalOutlined/>} placeholder={translate('个人网站', 'Website')}/>
        </Form.Item>
        <Form.Item className="comment-edit-submit" shouldUpdate>
          {() => (
              <Button type="primary" htmlType="submit" disabled={!editor}>{translate('提交评论', 'Post comment')}</Button>
          )}
        </Form.Item>
      </Form>
    </div>

  </>)

}


export default connect(
    userSessionOptionsMapStateToProps, { updateUserSession }
)(CommentEdit)
