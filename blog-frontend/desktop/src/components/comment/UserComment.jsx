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

import { Button, Empty, message, Pagination, Popconfirm, Skeleton, Table, Tag, Tooltip } from 'antd';
import moment from 'moment';
import React from 'react';
import { commentService } from '../../services';
import { isNotEmpty } from '../../utils';
import { connect } from "react-redux";
import { optionsMapStateToProps } from "../../redux/action-types";
import { updateUserSession } from "../../redux/actions";

class UserComment extends React.Component {

  state = {
    loaded: false,
    comments: {
      all: 0,
      size: 10,
      data: [],
      current: 1
    }
  }

  componentDidMount() {
    this.loadComment()
  }

  loadComment = (page = 1, size = 10) => {
    this.setState({ loaded: false })
    commentService.getByUser(page, size).then(res => {
      this.setState({ comments: res.data, loaded: true })
    }).catch(err => {
      const { status } = err.response || { status: 0 }
      if (status === 403) {
        message.error(err.response.data.message)
      }
      else if (status === 401) {
        this.props.updateUserSession(null)
      }
      else if (status === 404) {
        this.props.history.push("/not-found");
      }
      else {
        this.props.history.push("/internal-server-error");
      }
    })
  }

  deleteComment = (id) => {

    commentService.deleteComment(id).then(res => {
      if (res.data.success === true) {
        message.success(res.data.message)
        this.loadComment(this.state.comments.current, this.state.comments.size)
      }
      else {
        message.error(res.data.message)
      }
    }).catch(err => {
      const { status } = err.response || { status: 0 }
      if (status === 401) {
        this.props.updateUserSession(null)
      }
      else {
        message.error(err.response.data.message)
      }
    })
  }

  renderStatus(comment) {
    // CHECKED(0, "已审核"),
    // CHECKING(1, "未审核"),
    // RECYCLE(2, "回收站");
    switch (comment.status) {
      case 'CHECKED':
        return <a target="_blank" href={ `/articles/${ comment.articleId }` }>{ comment.articleId }</a>
      case 'RECYCLE':
        return <Tag className="btn btn-primary btn-xs">已经丢入垃圾桶</Tag>
      case 'CHECKING':
      default:
        return <Tag className="btn btn-primary btn-xs">等待审核</Tag>
    }
  }

  render() {
    const { loaded, comments } = this.state;
    if (loaded === false) {
      return (
          <div className="shadow-box">
            <div className="data-list-title"><em>正在加载</em> 我的评论</div>
            <Skeleton active/>
          </div>
      )
    }
    if (!comments) {
      return (
          <div className="shadow-box">
            <div className="data-list-title">我的评论</div>
            <Empty/>
          </div>
      )
    }

    const columns = [
      {
        title: '内容',
        key: 'content',
        render: (text, comment, _) => {
          return <div className="markdown"
                      dangerouslySetInnerHTML={ { __html: comment.content } }/>
        }
      },
      {
        title: '页面',
        key: 'page',
        render: (text, comment, _) => {
          return this.renderStatus(comment)
        }
      },
      {
        title: '时间',
        key: 'time',
        render: (text, comment, _) => {
          return <>
            <Tooltip title={ moment(comment.createAt).format("llll") }>
              <span style={ { cursor: 'pointer' } }>
                <time> { moment(comment.createAt).fromNow() } </time>
              </span>
            </Tooltip>
          </>
        }
      },
      {
        title: '操作',
        key: 'operation',
        render: (text, comment, index) => {
          return <>
            <Popconfirm className="btn btn-danger btn-xs" title="删除不可恢复" onConfirm={ () => this.deleteComment(comment.id) }>
              <Button danger size="small">删除</Button>
            </Popconfirm>
          </>
        }
      }
    ];

    return (<>
      <div className="shadow-box">
        <div className="data-list-title">我的评论</div>
        <div style={ { overflow: "auto" } }>
          <Table dataSource={ comments.data } columns={ columns } pagination={ false } rowKey="id"/>
        </div>
        { isNotEmpty(comments.data) &&
        <div align='center' style={ { padding: '20px' } }>
          <Pagination
              total={ comments.total }
              onChange={ this.loadComment.bind(this) }
              current={ comments.current }
              showTotal={ n => <>总共 <b className='red'>{ n }</b> 条评论</> }
          />
        </div>
        }
      </div>
    </>)
  }
}

export default connect(
    optionsMapStateToProps, { updateUserSession }
)(UserComment)
