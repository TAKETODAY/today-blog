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

import { useCallback, useMemo, useRef, useState } from 'react';
import { articleMessage as t } from '@/utils/article-i18n';
import { Divider, message, Popconfirm, Popover, Tag } from 'antd';
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
  const hide = message.loading(t('updating'))
  return update(id, comment)
      .finally(hide)
      .then(() => message.success(t('updateSuccess'), 1))
      .catch(() => message.error(t('updateFailed')))
}

/**
 * 删除
 *
 * @param comment
 */
const handleRemove = async (comment: CommentItem) => {
  const hide = message.loading(t('deleting'))
  return deleteComment(comment)
      .finally(hide)
      .then(() => message.success(t('deleteSuccess'), 1))
      .catch(() => message.error(t('deleteFailed')))
}

const renderStatusMenu = (comment: CommentItem, reload: () => void) => {

  const toggleStatus = async (status: string) => {
    return updateStatus(comment.id, status)
        .then(() => message.success(t('statusSuccess'), 0.75))
        .then(reload)
        .catch(() => message.error(t('statusFailed')))
  }

  const renderCHECKED = () => {
    return (<>
      <Popconfirm title={t('confirmUnapprove')} placement="topLeft" onConfirm={() => toggleStatus('CHECKING')}>
        <a href="#">{t('unapprove')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('confirmRecycleComment')} placement="topLeft" onConfirm={() => toggleStatus('RECYCLE')}>
        <a href="#">{t('recycle')}</a>
      </Popconfirm>
    </>)
  }

  const renderRECYCLE = () => {
    return (<>
      <Popconfirm title={t('confirmApprove')} placement="topLeft" onConfirm={() => toggleStatus('CHECKED')}>
        <a href="#">{t('approve')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('confirmUnapprove')} placement="topLeft" onConfirm={() => toggleStatus('CHECKING')}>
        <a href="#">{t('unapprove')}</a>
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
        <Popconfirm title={t('confirmApprove')} placement="topLeft" onConfirm={() => toggleStatus('CHECKED')}>
          <a href="#">{t('approve')}</a>
        </Popconfirm>
        <Divider type="vertical"/>
        <Popconfirm title={t('confirmRecycleComment')} placement="topLeft" onConfirm={() => toggleStatus('RECYCLE')}>
          <a href="#">{t('recycle')}</a>
        </Popconfirm>
      </>
  )
}

export default () => {
  const actionRef = useRef<ActionType>()
  const [updateComment, setUpdateComment] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)


  const remove = useCallback(async (record: CommentItem) => {
    return handleRemove(record)
        .then(() => actionRef.current?.reload())
  }, [])

  const reload = useCallback(async () => {
    await actionRef.current?.reload()
  }, [])

  const columns: ProColumns<CommentItem>[] = useMemo(() => [
    {
      title: t('commentContent'),
      width: 250,
      hideInSearch: true,
      render: (_, comment) => (
          <Popover title={t('commentTitle', { title: comment.articleTitle })} trigger="hover"
                   content={<div style={{ maxWidth: 1000, height: 'auto', overflowY: "auto" }}
                                 dangerouslySetInnerHTML={{ __html: comment.content }}/>}>
            <span>{computeSummary(comment.content)?.substring(0, 70)}</span>
          </Popover>
      )
    },
    {
      title: t('status'),
      width: 80,
      dataIndex: 'status',
      valueEnum: {
        "CHECKED": { text: t('checked') },
        "CHECKING": { text: t('checking') },
        "RECYCLE": { text: t('recycleBin') }
      },
      render: (_, comment: CommentItem) => {
        const statusDesc = t(comment.status === 'CHECKED' ? 'checked' : comment.status === 'CHECKING' ? 'checking' : 'recycleBin')
        return <Tag title={statusDesc}>{statusDesc}</Tag>
      }
    },
    {
      title: t('nickname'),
      width: 120,
      dataIndex: 'commenter',
    },
    {
      title: t('email'),
      width: 180,
      dataIndex: 'email',
    },
    {
      title: t('website'),
      width: 150,
      dataIndex: 'commenterSite',
      hideInForm: true
    },
    {
      title: t('article'),
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
      title: t('time'),
      width: 180,
      hideInSearch: true,
      render: (_, comment: CommentItem) => (format(comment.createAt, "lll"))
    },
    {
      title: t('actions'),
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
            >{t('edit')}</a>
            <Divider type="vertical"/>
            <Popconfirm title={t('confirmDeleteComment')} onConfirm={() => remove(record)}>
              <a href="#">{t('delete')}</a>
            </Popconfirm>
          </>
      ),
    },
  ], [])

  return (
      <PageContainer>
        <ProTable<CommentItem>
            rowKey="id"
            headerTitle={t('comments')}
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
