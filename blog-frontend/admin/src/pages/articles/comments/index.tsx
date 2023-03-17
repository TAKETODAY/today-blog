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
import { Divider, message, Popconfirm, Tag } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'

import CommentUpdateForm from "./CommentUpdateForm";

import { CommentItem } from "../data.d";
import { deleteComment, queryComments, update } from "./service";
import { getCommentStatusDesc } from "@/utils";
import moment from "moment";


/**
 * 更新节点
 */
const handleUpdate = async (id: number, comment: CommentItem) => {
  const hide = message.loading(`正在更新: ${comment.id}`)
  try {
    await update(id, comment)
    hide()
    message.success('更新信息成功', 1)
    return true
  }
  catch (error) {
    hide()
    message.error('更新信息失败请重试！')
    return false
  }
}

/**
 *  删除节点
 * @param comment
 */
const handleRemove = async (comment: CommentItem) => {
  const hide = message.loading(`正在删除`)
  try {
    await deleteComment(comment)
    hide()
    message.success('删除成功，即将刷新', 1)
    return true
  }
  catch (error) {
    hide()
    message.error('删除失败，请重试')
    return false
  }
}

export default () => {
  const actionRef = useRef<ActionType>()
  const [updateComment, setUpdateComment] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)

  const remove = async (record: CommentItem) => {
    await handleRemove(record)
    await actionRef.current?.reload()
  }

  const columns: ProColumns<CommentItem>[] = [
    {
      title: '评论内容',
      render: (_, comment: CommentItem) => {
        return <div dangerouslySetInnerHTML={{ __html: comment.content }}/>
      }
    },
    {
      title: '文章',
      dataIndex: 'articleId',
      hideInSearch: true,
      hideInForm: true
    },
    {
      title: '状态',
      width: 80,
      render: (_, comment: CommentItem) => {
        const statusDesc = getCommentStatusDesc(comment.status)
        return <Tag title={statusDesc}>{statusDesc}</Tag>
      }
    },
    {
      title: '时间',
      width: 180,
      render: (_, comment: CommentItem) => (moment(comment.id).format("lll"))
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record: CommentItem) => (
          <>
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
        />
        <CommentUpdateForm
            onSubmit={async (id: number, value: CommentItem) => {
              if (await handleUpdate(id, value)) {
                setUpdateComment({})
                setUpdateModalVisible(false)
                await actionRef.current?.reload()
              }
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
