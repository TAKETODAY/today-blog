import React, { useRef, useState } from 'react';
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

  const reload = () => {
    // @ts-ignore
    actionRef.current?.reloadAndRest()
  }

  const remove = async (record: LabelItem) => {
    await handleRemove(record)
    reload()
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
            rowSelection={{
            }}
        />
        <CreateForm title="新建文章标签" onCancel={() => setCreateModalVisible(false)} modalVisible={createModalVisible}>
          <ProTable<LabelItem, LabelItem>
              onSubmit={async (value) => {
                if (await handleCreate(value)) {
                  setCreateModalVisible(false);
                  reload();
                }
              }}
              rowKey="key"
              type="form"
              columns={columns}
              rowSelection={{}}
          />
        </CreateForm>
        <LabelUpdateForm
            onSubmit={async (value: LabelItem) => {
              if (await handleUpdate(value)) {
                setUpdateLabel({})
                setUpdateModalVisible(false)
                reload()
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
