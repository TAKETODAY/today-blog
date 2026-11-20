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
import { Divider, message, Popconfirm } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { getUserStatusDesc } from "@/utils";

import UserUpdateForm from "./UserUpdateForm";

import { UserItem } from "./data.d";
import { deleteUser, queryUsers, toggleStatus, update } from "./service";
import Image from "@/components/Image";
import { siteText } from '@/utils/site-i18n';
import { useSiteTranslation } from '@/utils/use-site-translation';

/**
 * 更新节点
 * @param user
 */
const handleUpdate = async (user: UserItem) => {
  const hide = message.loading(siteText('users.updating', { name: user.name }))
  try {
    await update(user)
    hide()
    message.success(siteText('users.userUpdated'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(siteText('users.couldNotUpdateTheUser'))
    return false
  }
}

/**
 *  删除节点
 * @param user
 */
const handleRemove = async (user: UserItem) => {
  const hide = message.loading(siteText('users.deleting', { name: user.name }))
  try {
    await deleteUser(user)
    hide()
    message.success(siteText('users.userDeleted'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(siteText('users.couldNotDeleteTheUser'))
    return false
  }
}


const renderStatusMenu = (user: UserItem, reload: Function, t: ReturnType<typeof useSiteTranslation>) => {
  const userStatus = async (status: string) => {
    const hide = message.loading(t('users.changingStatus', { name: user.name }))
    try {
      await toggleStatus(user.id, status)
      hide()
      message.success(t('users.userStatusChanged'), 1)
      reload()
      return true
    }
    catch (error) {
      hide()
      message.error(t('users.couldNotChangeTheStatus'))
      return false
    }
  }

  const renderNormal = () => {
    return (<>
      <Popconfirm title={t('users.lockThisUser')} placement="topLeft" onConfirm={() => userStatus('LOCKED')}>
        <a href="#">{t('users.lock')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('users.suspendThisUser')} placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">{t('users.suspend')}</a>
      </Popconfirm>
    </>)
  }

  const renderInactive = () => {
    return (<>
      <Popconfirm title={t('users.activateThisUser')} placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
        <a href="#">{t('users.activate')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('users.suspendThisUser')} placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">{t('users.suspend')}</a>
      </Popconfirm>
    </>)
  }
  const renderLocked = () => {
    return (<>
      <Popconfirm title={t('users.restoreThisUserToActive')} placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
        <a href="#">{t('users.restore')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('users.suspendThisUser')} placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">{t('users.suspend')}</a>
      </Popconfirm>
    </>)
  }
  if (user.status === 'NORMAL') {
    return renderNormal()
  }
  else if (user.status === 'INACTIVE') {
    return renderInactive()
  }
  else if (user.status === 'LOCKED') {
    return renderLocked()
  }

  return (
      <>
        <Popconfirm title={t('users.restoreThisUserToActive')} placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
          <a href="#">{t('users.restore')}</a>
        </Popconfirm>
        <Divider type="vertical"/>
        <Popconfirm title={t('users.lockThisUser')} placement="topLeft" onConfirm={() => userStatus('LOCKED')}>
          <a href="#">{t('users.lock')}</a>
        </Popconfirm>
      </>
  )
}

export default () => {
  const t = useSiteTranslation();
  const actionRef = useRef<ActionType>()
  const [updateUser, setUpdateUser] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)

  const reload = () => {
    // @ts-ignore
    actionRef.current?.reloadAndRest()
  }

  const remove = async (record: UserItem) => {
    await handleRemove(record)
    reload()
  }

  const columns: ProColumns<UserItem>[] = [
    {
      title: t('users.name'),
      dataIndex: 'name',
      width: 120,
      fixed: 'left',
    },
    {
      title: t('users.avatar'),
      hideInSearch: true,
      width: 80,
      render: (_, user) => (
          <Image title={user.name} src={user.avatar} width={68} alt={user.name} original={false}/>
      ),
    },
    {
      title: t('users.background'),
      dataIndex: 'background',
      hideInSearch: true,
      width: 180,
      render: (_, user) => (
          <Image src={user.background} height="68" alt={user.name} original={false}/>
      ),
    },
    {
      title: t('users.email'),
      dataIndex: 'email',
    },
    {
      title: t('users.bio'),
      dataIndex: 'introduce',
      hideInSearch: true,
    },
    {
      title: t('users.personalSite'),
      dataIndex: 'site',
      hideInSearch: true,
      width: 150,
      render: (_, user) => <a href={user.site} target='_blank'>{user.site}</a>,
    },
    {
      title: t('users.registrationType'),
      dataIndex: 'type',
      hideInSearch: true,
      width: 80,
    },
    {
      title: t('users.notifications'),
      dataIndex: 'notification',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{user.notification ? t('users.enabled') : t('users.disabled')}</span>,
    },
    {
      title: t('users.defaultPassword'),
      dataIndex: 'defaultPassword',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{user.defaultPassword ? t('users.unchanged') : t('users.changed')}</span>,
    },
    {
      title: t('users.status'),
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{getUserStatusDesc(user.status)}</span>,
    },
    {
      title: t('users.actions'),
      valueType: 'option',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateUser(record)
              setUpdateModalVisible(true)
            }}
            >{t('users.edit')}</a>
            <Divider type="vertical"/>
            <Popconfirm title={t('users.deleteThisUser')} placement="topLeft" onConfirm={() => remove(record)}>
              <a href="#">{t('users.delete')}</a>
            </Popconfirm>
            <Divider type="vertical"/>
            {
              renderStatusMenu(record, reload, t)
            }
          </>
      ),
    },
  ]

  return (
      <PageContainer>
        <ProTable<UserItem>
            rowKey="id"
            headerTitle={t('users.users')}
            columns={columns}
            request={queryUsers}
            actionRef={actionRef}
            scroll={{ x: 1500 }}
            rowSelection={{}}
        />

        <UserUpdateForm
            onSubmit={async (value: UserItem) => {
              if (await handleUpdate(value)) {
                setUpdateUser({})
                setUpdateModalVisible(false)
                reload()
              }
            }}
            onCancel={() => {
              setUpdateUser({})
              setUpdateModalVisible(false)
            }}
            updateModalVisible={updateModalVisible}
            values={updateUser}
        />
      </PageContainer>
  )
}
