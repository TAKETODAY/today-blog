import React, { useRef, useState } from 'react';
import { Button, Divider, message, Popconfirm } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { PlusOutlined } from '@ant-design/icons';

import CategoryUpdateForm from "./CategoryUpdateForm";

import { CategoryItem } from "../data.d";
import { create, deleteCategory, queryCategories, update } from "./service";
import CreateForm from "@/pages/articles/components/CreateForm";


/**
 * 更新节点
 * @param oldName
 * @param category
 */
const handleUpdate = async (oldName: string, category: CategoryItem) => {
  const hide = message.loading(`正在更新: ${oldName}`)
  try {
    await update(oldName, category)
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
 * @param category
 */
const handleRemove = async (category: CategoryItem) => {
  const hide = message.loading(`正在删除: ${category.name}`)
  try {
    await deleteCategory(category)
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
const handleCreate = async (category: CategoryItem) => {
  const hide = message.loading(`正在创建: ${category.name}`)
  try {
    await create(category)
    hide()
    message.success('创建文章分类成功，即将刷新', 1)
    return true
  }
  catch (error) {
    hide()
    message.error('创建文章分类失败，请重试')
    return false
  }
}

export default () => {
  const actionRef = useRef<ActionType>()
  const [updateCategory, setUpdateCategory] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);

  const remove = async (record: CategoryItem) => {
    await handleRemove(record)
    actionRef.current?.reload()
  }

  const columns: ProColumns<CategoryItem>[] = [
    {
      title: '名称',
      dataIndex: 'name',
      hideInSearch: true,
      rules: [
        {
          required: true,
          message: '名称为必填项',
        },
      ],
    },
    {
      title: '排序',
      dataIndex: 'order',
      valueType: 'digit',
      hideInSearch: true,
      rules: [
        {
          required: true,
          message: '排序为必填项',
        },
      ],
    },
    {
      title: '文章数',
      dataIndex: 'articleCount',
      hideInSearch: true,
      hideInForm: true
    },
    {
      title: '描述',
      dataIndex: 'description',
      hideInSearch: true,
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateModalVisible(true)
              setUpdateCategory({ ...record })
            }}
            > 编辑</a>
            <Divider type="vertical"/>
            <Popconfirm title="您确定要删除该文章吗" onConfirm={() => remove(record)}>
              <a href="#">删除</a>
            </Popconfirm>
          </>
      ),
    },
  ]

  return (
      <PageContainer>
        <ProTable<CategoryItem>
            rowKey="name"
            headerTitle="文章分类列表"
            actionRef={actionRef}
            request={queryCategories}
            columns={columns}
            toolBarRender={() => [
              <Button type="primary" onClick={() => setCreateModalVisible(true)}>
                <PlusOutlined/> 新建
              </Button>]
            }
            rowSelection={{
            }}
        />
        <CreateForm title="新建文章分类" onCancel={() => setCreateModalVisible(false)} modalVisible={createModalVisible}>
          <ProTable<CategoryItem, CategoryItem>
              onSubmit={async (value) => {
                if (await handleCreate(value)) {
                  setCreateModalVisible(false);
                  actionRef.current?.reload();
                }
              }}
              rowKey="key"
              type="form"
              columns={columns}
              rowSelection={{}}
          />
        </CreateForm>
        <CategoryUpdateForm
            onSubmit={async (oldName: string, value: CategoryItem) => {
              if (await handleUpdate(oldName, value)) {
                setUpdateCategory({})
                setUpdateModalVisible(false)
                actionRef.current?.reload()
              }
            }}
            onCancel={() => {
              setUpdateCategory({})
              setUpdateModalVisible(false)
            }}
            updateModalVisible={updateModalVisible}
            values={updateCategory}
        />
      </PageContainer>
  )
}
