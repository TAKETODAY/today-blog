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

import { Empty, message, Skeleton } from 'antd';
import React from 'react';
import { commentService } from '../../services';
import { extractData, isEmpty, isNotEmpty, scrollTo } from '../../utils';
import { CommentEdit, CommentList } from '..';

export default class ArticleComment extends React.Component {

  state = {
    comments: null,
    commentsLoaded: false
  }

  onReply = (comment) => {
    // console.log("parent 回复用户-> ", user)
    // console.log("parent 回复评论-> ", commentId)
    this.setState({ replying: comment, commentId: comment.id })
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
    const articleId = this.props.articleId
    commentService.fetchComments(articleId, page)
      .then(extractData)
      .then(comments => {
        scrollTo('div#comments.data_list.markdown')
        console.log(comments)
        this.setState({ comments, commentsLoaded: true })
      })
      .catch(res => {
        console.log(res)
        message.error("评论加载失败")
      })
  }

  render() {
    const { comments, replying, commentId, commentsLoaded } = this.state
    const articleId = this.props.articleId

    return (<>
      <div className="shadow-box markdown" id="comment_area">
        <CommentEdit articleId={articleId} commentId={commentId}
                     replying={replying} cancelReply={this.cancelReply}/>
      </div>
      <div className="shadow-box markdown" id="comments">
        <div className="data-list-title" style={{ border: 'none' }}>
          <i className="fa fa-commenting-o"/> 评论
          {commentsLoaded
            ? isEmpty(comments) && <Empty description="暂无评论"/>
            : <Skeleton active/>
          }
        </div>
        {isNotEmpty(comments) && <>
          <CommentList data={comments} onReply={this.onReply}/>
          {isNotEmpty(comments) &&
            <div align='center' style={{ padding: '20px' }}>
              总共 <b className='red'>{comments.length}</b> 条评论
            </div>
          }</>
        }
      </div>
    </>)
  }

}

