import React, { useEffect, useRef, useState } from 'react';
import { Button, Divider, message, Popconfirm, Popover } from 'antd';
import { FooterToolbar, PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { isEmpty, isNotEmpty } from "@/utils";


import { ArticleItem, CategoryItem } from "./data.d";
import { deleteArticle, getCategories, queryArticles, toggleArticleStatus } from "./service";
import { PlusOutlined } from "@ant-design/icons/lib";
import moment from "moment";
import { Link } from "react-router-dom";


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

const renderStatusMenu = (article: ArticleItem, reload: Function) => {
  const toggleStatus = async (status: string) => {
    const hide = message.loading(`正在切换状态: ${article.title}`)
    try {
      await toggleArticleStatus(article.id, status)
      hide()
      message.success('切换状态成功，即将刷新', 1)
      reload()
      return true
    }
    catch (error) {
      hide()
      message.error('切换状态失败，请重试')
      return false
    }
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
        <Popconfirm title="您确定要将定该文章作为草稿吗" placement="topLeft" onConfirm={() => toggleStatus('DRAFT')}>
          <a href="#">草稿</a>
        </Popconfirm>
      </>
  )
}

export default () => {
  const actionRef = useRef<ActionType>()
  const [selectedBooks, setSelectedBooks] = useState<ArticleItem[]>([])
  const [categories, setCategories] = useState<CategoryItem[]>([])

  const reload = () => {
    actionRef.current?.reload()
  }

  const remove = async (record: ArticleItem) => {
    await handleRemove(record)
    reload()
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
      title: '标题',
      dataIndex: 'title',
      render: (_, article) => (
          <Popover title="文章标题" content={<span dangerouslySetInnerHTML={{ __html: article.title }}/>}>
            <a href={`/articles/${article.id}`} target='_blank'>{article.title.substr(0, 15)}...</a>
          </Popover>
      )
    },
    {
      title: '封面',
      dataIndex: 'image',
      hideInSearch: true,
      render: (_, article) => (
          article.image && <Popover placement="rightTop" title={article.title}
                                    content={<img src={article.image} alt={article.title}/>}>
            <img src={article.image} height={68} alt={article.title}/>
          </Popover>
      ),
    },
    {
      title: '分类',
      dataIndex: 'category',
      valueEnum: categoriesEnum,
    },
    {
      title: '内容',
      dataIndex: 'content',
      render: (_, article) => (
          <Popover title={article.title} trigger="hover"
                   content={<span dangerouslySetInnerHTML={{ __html: article.content }}/>}>
            <span>{article.summary?.substr(0, 8)}...</span>
          </Popover>
      )
    },
    {
      title: '浏览量',
      dataIndex: 'pv',
      sorter: true,
      hideInSearch: true
    },
    {
      title: '密码',
      dataIndex: 'password',
      hideInSearch: true
    },
    {
      title: '发表日期',
      dataIndex: 'id',
      sorter: true,
      valueType: 'dateTimeRange',
      render: (_, record) => (
          <>
            {moment(record.id).format('lll')}
          </>
      ),
    },
    {
      title: '最后更改',
      sorter: true,
      dataIndex: 'lastModify',
      valueType: 'dateTimeRange',
      render: (_, record) => (
          <>
            {moment(record.lastModify).format('lll')}
          </>
      ),
    },
    {
      title: '操作',
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
            toolBarRender={() => [
              <Link to='/articles/write' target='_blank'>
                <PlusOutlined/> 新建
              </Link>
            ]}
            rowSelection={{
              onChange: (_, sd) => setSelectedBooks(sd),
            }}

        />
        {isNotEmpty(selectedBooks) && (
            <FooterToolbar
                extra={
                  <div>已选择 <a style={{ fontWeight: 600 }}>{selectedBooks.length}</a> 项</div>
                }
            >
              <Button onClick={() => setSelectedBooks([])}>取消</Button>
              <Popconfirm title="还不支持批量操作">
                <Button>批量删除</Button>
              </Popconfirm>
              <Popconfirm title="还不支持批量操作" placement="topLeft">
                <Button type="primary">批量锁定</Button>
              </Popconfirm>
            </FooterToolbar>
        )}

      </PageContainer>
  )
}
