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

import { useEffect, useRef, useState } from 'react';
import { Button, Divider, message, Popconfirm, Popover } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { format, isEmpty } from "@/utils";


import { ArticleItem, CategoryItem } from "./data.d";
import { deleteArticle, getCategories, queryArticles, updateStatus } from "./service";
import { PlusOutlined } from "@ant-design/icons/lib";
import { Link } from "react-router-dom";
import { ArticleLink } from "@/components/Article";
import Image from "@/components/Image";


/**
 *  删除节点
 * @param article
 */
const handleRemove = async (article: ArticleItem) => {
  const hide = message.loading(`正在删除: ${article.title}`)
  try {
    await deleteArticle(article)
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

const renderStatusMenu = (article: ArticleItem, reload: () => void) => {

  const toggleStatus = async (status: string) => {
    return updateStatus(article.id, status)
        .then(reload)
        .then(() => message.success('切换状态成功，即将刷新', 1))
        .catch(() => message.error('切换状态失败，请重试'))
  }

  const renderPublished = () => {
    return (<>
      <Popconfirm title="您确定要将定该文章作为草稿吗" placement="topLeft" onConfirm={() => toggleStatus('DRAFT')}>
        <a href="#">草稿</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要回收该文章吗" placement="topLeft" onConfirm={() => toggleStatus('RECYCLE')}>
        <a href="#">回收</a>
      </Popconfirm>
    </>)
  }

  const renderDraft = () => {
    return (<>
      <Popconfirm title="您确定要发布该文章吗" placement="topLeft" onConfirm={() => toggleStatus('PUBLISHED')}>
        <a href="#">发布</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要回收该文章吗" placement="topLeft" onConfirm={() => toggleStatus('RECYCLE')}>
        <a href="#">回收</a>
      </Popconfirm>
    </>)
  }

  if (article.status === 'PUBLISHED') {
    return renderPublished()
  }
  else if (article.status === 'DRAFT') {
    return renderDraft()
  }

  return (
      <>
        <Popconfirm title="您确定要发布该文章吗" placement="topLeft" onConfirm={() => toggleStatus('PUBLISHED')}>
          <a href="#">发布</a>
        </Popconfirm>
        <Divider type="vertical"/>
        <Popconfirm title="您确定要将该文章作为草稿吗" placement="topLeft" onConfirm={() => toggleStatus('DRAFT')}>
          <a href="#">草稿</a>
        </Popconfirm>
      </>
  )
}

export default () => {
  const actionRef = useRef<ActionType>()
  const [categories, setCategories] = useState<CategoryItem[]>([])

  const reload = async () => {
    await actionRef.current?.reload()
  }

  const remove = async (record: ArticleItem) => {
    await handleRemove(record)
    await reload()
  }

  useEffect(() => {
    getCategories().then(res => {
      setCategories(res.data)
    })
  }, [])

  const categoriesEnum = {}

  categories.forEach(category => {
    const { name } = category
    categoriesEnum[name] = { text: name }
  })

  const columns: ProColumns<ArticleItem>[] = [
    {
      width: 140,
      title: '标题',
      fixed: 'left',
      dataIndex: 'title',
      render: (_, article) => (
          <Popover title="文章标题" content={<span dangerouslySetInnerHTML={{ __html: article.title }}/>}>
            <ArticleLink article={article}>
              {article.title?.length > 10 ? `${article.title.substring(0, 15)}...` : article.title}
            </ArticleLink>
          </Popover>
      )
    },
    {
      title: '封面',
      width: 120,
      dataIndex: 'cover',
      hideInSearch: true,
      render: (_, article) => (
          <Image title={article.title} src={article.cover} width={120} original={false}/>
      ),
    },
    {
      title: '分类',
      width: 100,
      dataIndex: 'category',
      valueEnum: categoriesEnum,
    },
    {
      title: '内容',
      width: 250,
      dataIndex: 'content',
      render: (_, article) => (
          <Popover title={article.title} trigger="hover"
                   content={<div style={{ maxWidth: 1000, height: 550, overflowY: "auto" }}
                                 dangerouslySetInnerHTML={{ __html: article.content }}/>}>
            <span>{article.summary?.substring(0, 70)}...</span>
          </Popover>
      )
    },
    {
      title: '浏览量',
      width: 80,
      dataIndex: 'pv',
      sorter: true,
      hideInSearch: true
    },
    {
      title: '密码',
      width: 100,
      dataIndex: 'password',
      hideInSearch: true
    },
    {
      title: '发表日期',
      dataIndex: 'createAt',
      width: 170,
      sorter: true,
      valueType: 'dateTimeRange',
      render: (_, record) => (
          <>
            {format(record.createAt, 'lll')}
          </>
      ),
    },
    {
      width: 170,
      title: '最后更改',
      sorter: true,
      dataIndex: 'updateAt',
      valueType: 'dateTimeRange',
      render: (_, record) => (
          <>
            {format(record.updateAt, 'lll')}
          </>
      ),
    },
    {
      width: 180,
      title: '操作',
      fixed: 'right',
      valueType: 'option',
      render: (_, record) => (
          <>
            <Link to={isEmpty(record.markdown) ? `/articles/${record.id}/modify-rich-text` : `/articles/${record.id}/modify`}
                  target='_blank'>修改</Link>
            <Divider type="vertical"/>
            <Popconfirm title="您确定要删除该文章吗" placement="topLeft" onConfirm={() => remove(record)}>
              <a href="#">删除</a>
            </Popconfirm>
            <Divider type="vertical"/>
            {
              renderStatusMenu(record, reload)
            }
          </>
      ),
    },
  ]

  return (
      <PageContainer>
        <ProTable<ArticleItem>
            rowKey="id"
            headerTitle="博客文章列表"
            actionRef={actionRef}
            request={queryArticles}
            columns={columns}
            scroll={{ x: 1200 }}
            toolBarRender={() => [
              <Button type="primary">
                <Link to="/articles/write-rich-text" target="_blank">
                  <PlusOutlined/> 富文本
                </Link>
              </Button>,
              <Button type="primary">
                <Link to="/articles/write" target="_blank">
                  <PlusOutlined/> Markdown 格式
                </Link>
              </Button>
            ]}
        />

      </PageContainer>
  )
}
