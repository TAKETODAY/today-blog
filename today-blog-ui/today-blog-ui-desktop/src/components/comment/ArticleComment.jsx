import { Empty, message, Pagination, Skeleton } from 'antd';
import React from 'react';
import { commentService } from '../../services';
import { getArticleId, isEmpty, isNotEmpty, scrollTo } from '../../utils';
import { CommentEdit, CommentList } from '..';

export default class ArticleComment extends React.Component {

  state = {
    comments: {
      all: 0,
      current: 1,
      num: 0,
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
    commentService.getComments(articleId, page).then(res => {
      scrollTo('div#comments.data_list.markdown')
      this.setState({ comments: res.data, commentsLoaded: true })
    }).catch(res => {
      message.error("评论加载失败")
    })
  }

  render() {
    const { comments, replying, commentId } = this.state
    const articleId = getArticleId(this.props.articleId)

    return (<>
      <div className="data_list markdown" id="comment_area">
        <CommentEdit articleId={ articleId } commentId={ commentId } replying={ replying } cancelReply={ this.cancelReply }/>
      </div>
      <div className="data_list markdown" id="comments">
        <div className="data_list_title" style={ { border: 'none' } }>
          <i className="fa fa-commenting-o"/> 评论信息
          { this.state.commentsLoaded
              ? isEmpty(comments.data) && <Empty description="暂无评论"/>
              : <Skeleton active/>
          }
        </div>
        { comments.data && <>
          <CommentList data={ comments.data } onReply={ this.onReply }/>
          { isNotEmpty(comments.data) &&
          <div align='center' style={ { padding: '20px' } }>
            <Pagination
                total={ comments.all }
                onChange={ this.loadComment.bind(this) }
                current={ comments.current }
                showTotal={ n => <>总共 <b className='red'>{ n }</b> 条评论</> }
            />
          </div>
          }</>
        }
      </div>
    </>)
  }

}

