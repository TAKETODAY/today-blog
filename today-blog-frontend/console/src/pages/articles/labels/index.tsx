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

import {useRef, useState} from 'react';
import { articleMessage as t } from '@/utils/article-i18n';
import {Button, Divider, message, Popconfirm} from 'antd';
import {PageContainer} from '@ant-design/pro-layout';
import ProTable, {ActionType, ProColumns} from '@ant-design/pro-table'
import {PlusOutlined} from '@ant-design/icons';

import LabelUpdateForm from "./LabelUpdateForm";
import CreateForm from "../components/CreateForm";

import {LabelItem} from "../data.d";
import {create, deleteLabel, queryTags, update} from "./service";


/**
 * 更新节点
 * @param label
 */
const handleUpdate = async (label: LabelItem) => {
  const hide = message.loading(t('updatingName', { name: label.name }))
  try {
    await update(label)
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
 * @param label
 */
const handleRemove = async (label: LabelItem) => {
  const hide = message.loading(t('deletingName', { name: label.name }))
  try {
    await deleteLabel(label)
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

const handleCreate = async (label: LabelItem) => {
  const hide = message.loading(t('creatingName', { name: label.name }))
  try {
    await create(label.name)
    hide()
    message.success(t('tagCreated'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(t('tagCreateFailed'))
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
      title: t('name'),
      dataIndex: 'name',
      hideInSearch: true,
    },
    {
      title: t('actions'),
      valueType: 'option',
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateModalVisible(true)
              setUpdateLabel({...record})
            }}
            >{t('edit')}</a>
            <Divider type="vertical"/>
            <Popconfirm title={t('confirmDeleteTag')} onConfirm={() => remove(record)}>
              <a href="#">{t('delete')}</a>
            </Popconfirm>
          </>
      ),
    },
  ]

  return (
      <PageContainer>
        <ProTable<LabelItem>
            rowKey="id"
            headerTitle={t('tags')}
            actionRef={actionRef}
            request={queryTags}
            columns={columns}
            toolBarRender={() => [
              <Button type="primary" onClick={() => setCreateModalVisible(true)}>
                <PlusOutlined/> {t('create')}
              </Button>]
            }
        />
        <CreateForm title={t('createTag')} onCancel={() => setCreateModalVisible(false)} modalVisible={createModalVisible}>
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
