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

import { Empty, message, Pagination, Skeleton } from 'antd';
import React from 'react';
import { commentService } from '../../services';
import { extractData, getArticleId, isEmpty, isNotEmpty, scrollTo } from '../../utils';
import { CommentEdit, CommentList } from '..';

export default class ArticleComment extends React.Component {

  state = {
    comments: {
      total: 0,
      current: 1,
      pages: 0,
      size: 8,
      data: []
    },
    commentsLoaded: false
  }

  onReply = (user, commentId) => {
    // console.log("parent 回复用户-> ", user)
    // console.log("parent 回复评论-> ", commentId)
    this.setState({ replying: user, commentId })
  }

  cancelReply = () => {
    this.setState({ replying: null, commentId: null })
  }

  intersectionObserver = null

  componentDidMount() {

    if (typeof window !== undefined && this.intersectionObserver === null) {
      this.intersectionObserver = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.intersectionRatio <= 0) {// If intersectionRatio is 0, the target is out of view and we do not need to do anything.
            return
          }
          if (!this.state.commentsLoaded) {
            this.loadComment(1)
            console.log('正在加载评论')
          }
        })
      }, { threshold: [1] })
    }
  }

  componentDidUpdate() {
    if (typeof window !== undefined && this.intersectionObserver !== null) {
      this.intersectionObserver.disconnect()
      this.intersectionObserver.observe(document.getElementById("comments"))
    }
  }

  componentWillUnmount() {
    this.intersectionObserver && this.intersectionObserver.disconnect()
  }

  loadComment = (page = 1) => {
    const articleId = getArticleId(this.props.articleId)
    commentService.getComments(articleId, page)
      .then(extractData)
      .then(comments => {
        scrollTo('div#comments.data_list.markdown')
        this.setState({ comments, commentsLoaded: true })
      })
      .catch(res => {
        message.error("评论加载失败")
      })
  }

  render() {
    const { comments, replying, commentId, commentsLoaded } = this.state
    const articleId = getArticleId(this.props.articleId)

    return (<>
      <div className="data_list markdown" id="comment_area">
        <CommentEdit articleId={articleId} commentId={commentId}
                     replying={replying} cancelReply={this.cancelReply}/>
      </div>
      <div className="data_list markdown" id="comments">
        <div className="data_list_title" style={{ border: 'none' }}>
          <i className="fa fa-commenting-o"/> 评论
          {commentsLoaded
            ? isEmpty(comments.data) && <Empty description="暂无评论"/>
            : <Skeleton active/>
          }
        </div>
        {comments.data && <>
          <CommentList data={comments.data} onReply={this.onReply}/>
          {isNotEmpty(comments.data) &&
            <div align='center' style={{ padding: '20px' }}>
              <Pagination
                total={comments.total}
                onChange={this.loadComment.bind(this)}
                current={comments.current}
                showTotal={n => <>总共 <b className='red'>{n}</b> 条评论</>}
              />
            </div>
          }</>
        }
      </div>
    </>)
  }

}

