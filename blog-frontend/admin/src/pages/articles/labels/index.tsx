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
import { Button, Divider, message, Popconfirm } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { PlusOutlined } from '@ant-design/icons';

import LabelUpdateForm from "./LabelUpdateForm";
import CreateForm from "../components/CreateForm";

import { LabelItem } from "../data.d";
import { create, deleteLabel, queryTags, update } from "./service";


/**
 * 更新节点
 * @param label
 */
const handleUpdate = async (label: LabelItem) => {
  const hide = message.loading(`正在更新: ${label.name}`)
  try {
    await update(label)
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
 * @param label
 */
const handleRemove = async (label: LabelItem) => {
  const hide = message.loading(`正在删除: ${label.name}`)
  try {
    await deleteLabel(label)
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

const handleCreate = async (label: LabelItem) => {
  const hide = message.loading(`正在创建: ${label.name}`)
  try {
    await create(label.name)
    hide()
    message.success('创建文章标签成功，即将刷新', 1)
    return true
  }
  catch (error) {
    hide()
    message.error('创建文章标签失败，请重试')
    return false
  }
}

export default () => {
  const actionRef = useRef<ActionType>()
  const [updateLabel, setUpdateLabel] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);

  const reload = async () => {
    // @ts-ignore
    await actionRef.current?.reloadAndRest()
  }

  const remove = async (record: LabelItem) => {
    await handleRemove(record)
    await reload()
  }

  const columns: ProColumns<LabelItem>[] = [
    {
      title: 'ID',
      dataIndex: 'id',
      hideInSearch: true,
      hideInForm: true
    },
    {
      title: '名称',
      dataIndex: 'name',
      hideInSearch: true,
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateModalVisible(true)
              setUpdateLabel({ ...record })
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
        <ProTable<LabelItem>
            rowKey="id"
            headerTitle="博客标签列表"
            actionRef={actionRef}
            request={queryTags}
            columns={columns}
            toolBarRender={() => [
              <Button type="primary" onClick={() => setCreateModalVisible(true)}>
                <PlusOutlined/> 新建
              </Button>]
            }
        />
        <CreateForm title="新建文章标签" onCancel={() => setCreateModalVisible(false)} modalVisible={createModalVisible}>
          <ProTable<LabelItem, LabelItem>
              onSubmit={async (value) => {
                if (await handleCreate(value)) {
                  setCreateModalVisible(false);
                  await reload();
                }
              }}
              rowKey="key"
              type="form"
              columns={columns}
          />
        </CreateForm>
        <LabelUpdateForm
            onSubmit={async (value: LabelItem) => {
              if (await handleUpdate(value)) {
                setUpdateLabel({})
                setUpdateModalVisible(false)
                await reload()
              }
            }}
            onCancel={() => {
              setUpdateLabel({})
              setUpdateModalVisible(false)
            }}
            updateModalVisible={updateModalVisible}
            values={updateLabel}
        />
      </PageContainer>
  )
}
