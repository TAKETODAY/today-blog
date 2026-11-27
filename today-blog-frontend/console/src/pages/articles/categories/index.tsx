/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2024 All Rights Reserved.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { useRef, useState } from 'react';
import { articleMessage as t } from '@/utils/article-i18n';
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
  const hide = message.loading(t('updatingName', { name: oldName }))
  try {
    await update(oldName, category)
    hide()
    message.success(t('updateSuccess'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(t('updateFailed'))
    return false
  }
}

/**
 *  删除节点
 * @param category
 */
const handleRemove = async (category: CategoryItem) => {
  const hide = message.loading(t('deletingName', { name: category.name }))
  try {
    await deleteCategory(category)
    hide()
    message.success(t('deleteSuccess'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(t('deleteFailed'))
    return false
  }
}
const handleCreate = async (category: CategoryItem) => {
  const hide = message.loading(t('creatingName', { name: category.name }))
  try {
    await create(category)
    hide()
    message.success(t('categoryCreated'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(t('categoryCreateFailed'))
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
    await actionRef.current?.reload()
  }

  const columns: ProColumns<CategoryItem>[] = [
    {
      title: t('name'),
      dataIndex: 'name',
      hideInSearch: true,
    },
    {
      title: t('order'),
      dataIndex: 'order',
      valueType: 'digit',
      hideInSearch: true,
    },
    {
      title: t('count'),
      dataIndex: 'articleCount',
      hideInSearch: true,
      hideInForm: true
    },
    {
      title: t('description'),
      dataIndex: 'description',
      hideInSearch: true,
    },
    {
      title: t('actions'),
      valueType: 'option',
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateModalVisible(true)
              setUpdateCategory({ ...record })
            }}
            >{t('edit')}</a>
            <Divider type="vertical"/>
            <Popconfirm title={t('confirmDeleteCategory')} onConfirm={() => remove(record)}>
              <a href="#">{t('delete')}</a>
            </Popconfirm>
          </>
      ),
    },
  ]

  return (
      <PageContainer>
        <ProTable<CategoryItem>
            rowKey="name"
            headerTitle={t('categories')}
            actionRef={actionRef}
            request={queryCategories}
            columns={columns}
            toolBarRender={() => [
              <Button type="primary" onClick={() => setCreateModalVisible(true)}>
                <PlusOutlined/> {t('create')}
              </Button>]
            }
        />
        <CreateForm title={t('createCategory')} onCancel={() => setCreateModalVisible(false)} modalVisible={createModalVisible}>
          <ProTable<CategoryItem, CategoryItem>
              onSubmit={async (value) => {
                if (await handleCreate(value)) {
                  setCreateModalVisible(false);
                  await actionRef.current?.reload();
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
                await actionRef.current?.reload()
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
