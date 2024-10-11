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

import { useRef, useState } from 'react';
import { Divider, message, Popconfirm, Popover, Select, Tag } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'

import CommentUpdateForm from "./CommentUpdateForm";

import { CommentItem } from "../data.d";
import { deleteComment, queryComments, update, updateStatus } from "./service";
import { computeSummary, format, getCommentStatusDesc } from "@/utils";
import { ArticleLink } from "@/components/Article";


/**
 * 更新
 */
const handleUpdate = async (id: number, comment: CommentItem) => {
  const hide = message.loading(`正在更新`)
  return update(id, comment)
      .finally(hide)
      .then(() => message.success('更新信息成功', 1))
      .catch(() => message.error('更新信息失败请重试！'))
}

/**
 * 删除
 *
 * @param comment
 */
const handleRemove = async (comment: CommentItem) => {
  const hide = message.loading(`正在删除`)
  return deleteComment(comment)
      .finally(hide)
      .then(() => message.success('删除成功，即将刷新', 1))
      .catch(() => message.error('删除失败，请重试'))
}

const renderStatusMenu = (comment: CommentItem, reload: () => void) => {

  const toggleStatus = async (status: string) => {
    return updateStatus(comment.id, status)
        .then(() => message.success('状态更新成功', 0.75))
        .then(reload)
        .catch(() => message.error('状态更新失败，请重试'))
  }

  const renderCHECKED = () => {
    return (<>
      <Popconfirm title="您确定要撤销审核该评论吗" placement="topLeft" onConfirm={() => toggleStatus('CHECKING')}>
        <a href="#">撤销审核</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要将该评论放进回收站吗" placement="topLeft" onConfirm={() => toggleStatus('RECYCLE')}>
        <a href="#">回收</a>
      </Popconfirm>
    </>)
  }

  const renderRECYCLE = () => {
    return (<>
      <Popconfirm title="您确定要发布该文章吗" placement="topLeft" onConfirm={() => toggleStatus('CHECKED')}>
        <a href="#">通过审核</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要撤销审核该评论吗" placement="topLeft" onConfirm={() => toggleStatus('CHECKING')}>
        <a href="#">撤销审核</a>
      </Popconfirm>
    </>)
  }

  if (comment.status === 'CHECKED') {
    return renderCHECKED()
  }
  else if (comment.status === 'RECYCLE') {
    return renderRECYCLE()
  }

  // CHECKING
  return (
      <>
        <Popconfirm title="您确定要审核通过该评论吗" placement="topLeft" onConfirm={() => toggleStatus('CHECKED')}>
          <a href="#">通过审核</a>
        </Popconfirm>
        <Divider type="vertical"/>
        <Popconfirm title="您确定要将该评论放进回收站吗" placement="topLeft" onConfirm={() => toggleStatus('RECYCLE')}>
          <a href="#">回收</a>
        </Popconfirm>
      </>
  )
}
export default () => {
  const actionRef = useRef<ActionType>()
  const [updateComment, setUpdateComment] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)

  const remove = async (record: CommentItem) => {
    handleRemove(record)
        .then(() => actionRef.current?.reload())
  }

  const reload = async () => {
    await actionRef.current?.reload()
  }

  const columns: ProColumns<CommentItem>[] = [
    {
      title: '评论内容',
      width: 250,
      render: (_, comment) => (
          <Popover title={`评论：《${comment.articleTitle}》`} trigger="hover"
                   content={<div style={{ maxWidth: 1000, height: 'auto', overflowY: "auto" }}
                                 dangerouslySetInnerHTML={{ __html: comment.content }}/>}>
            <span>{computeSummary(comment.content)?.substring(0, 70)}</span>
          </Popover>
      )
    },
    {
      title: '昵称',
      width: 120,
      dataIndex: 'commenter',
      hideInSearch: true,
    },
    {
      title: '邮箱',
      width: 180,
      dataIndex: 'email',
      hideInSearch: true,
    },
    {
      title: '个人网站',
      width: 150,
      dataIndex: 'commenterSite',
      hideInSearch: true,
      hideInForm: true
    },
    {
      title: '文章',
      width: 200,
      hideInSearch: true,
      hideInForm: true,
      render: (_, comment: CommentItem) => {
        return (
            <ArticleLink id={comment.articleId}>
              {comment.articleTitle?.length > 15 ? `${comment.articleTitle.substring(0, 15)}...` : comment.articleTitle}
            </ArticleLink>
        )
      }
    },
    {
      title: '状态',
      width: 80,
      dataIndex: 'status',
      render: (_, comment: CommentItem) => {
        const statusDesc = getCommentStatusDesc(comment.status)
        return <Tag title={statusDesc}>{statusDesc}</Tag>
      },
      renderFormItem(schema: { isEditable?: boolean; index?: number; originProps?: any; },
                     config, form, action) {
        return (
            <>
              <Select placeholder='请选择状态' options={[
                {
                  value: 'CHECKED',
                  label: '已审核',
                },
                {
                  value: 'CHECKING',
                  label: '未审核',
                },
                {
                  value: 'RECYCLE',
                  label: '回收站',
                }
              ]}
              >
              </Select>
            </>
        )
      }
    },
    {
      title: '时间',
      width: 180,
      hideInSearch: true,
      render: (_, comment: CommentItem) => (format(comment.createAt, "lll"))
    },
    {
      title: '操作',
      valueType: 'option',
      width: 220,
      fixed: 'right',
      render: (_, record: CommentItem) => (
          <>
            {
              renderStatusMenu(record, reload)
            }
            <Divider type="vertical"/>
            <a onClick={() => {
              setUpdateModalVisible(true)
              setUpdateComment({ ...record })
            }}
            > 编辑</a>
            <Divider type="vertical"/>
            <Popconfirm title="您确定要删除该评论吗？" onConfirm={() => remove(record)}>
              <a href="#">删除</a>
            </Popconfirm>
          </>
      ),
    },
  ]

  return (
      <PageContainer>
        <ProTable<CommentItem>
            rowKey="id"
            headerTitle="文章评论列表"
            actionRef={actionRef}
            request={queryComments}
            columns={columns}
            scroll={{ x: 1200 }}
        />
        <CommentUpdateForm
            onSubmit={async (id: number, value: CommentItem) => {
              return handleUpdate(id, value)
                  .then(() => {
                    setUpdateComment({})
                    setUpdateModalVisible(false)
                    return reload()
                  })
            }}
            onCancel={() => {
              setUpdateComment({})
              setUpdateModalVisible(false)
            }}
            updateModalVisible={updateModalVisible}
            values={updateComment}
        />
      </PageContainer>
  )
}
