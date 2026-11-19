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
import { siteText } from '@/utils/site-format';
import { useSiteTranslation } from '@/utils/use-site-translation';

/**
 * 更新节点
 * @param user
 */
const handleUpdate = async (user: UserItem) => {
  const hide = message.loading(siteText(`正在更新: ${user.name}`, `Updating: ${user.name}`))
  try {
    await update(user)
    hide()
    message.success(siteText('更新信息成功', 'User updated'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(siteText('更新信息失败请重试！', 'Could not update the user. Try again.'))
    return false
  }
}

/**
 *  删除节点
 * @param user
 */
const handleRemove = async (user: UserItem) => {
  const hide = message.loading(siteText(`正在删除: ${user.name}`, `Deleting: ${user.name}`))
  try {
    await deleteUser(user)
    hide()
    message.success(siteText('删除成功，即将刷新', 'User deleted'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(siteText('删除失败，请重试', 'Could not delete the user. Try again.'))
    return false
  }
}


const renderStatusMenu = (user: UserItem, reload: Function, t: (zh: string, en: string) => string) => {
  const userStatus = async (status: string) => {
    const hide = message.loading(t(`正在切换状态: ${user.name}`, `Changing status: ${user.name}`))
    try {
      await toggleStatus(user.id, status)
      hide()
      message.success(t('切换状态成功，即将刷新', 'User status changed'), 1)
      reload()
      return true
    }
    catch (error) {
      hide()
      message.error(t('切换状态失败，请重试', 'Could not change the status. Try again.'))
      return false
    }
  }

  const renderNormal = () => {
    return (<>
      <Popconfirm title={t('您确定要锁定该用户吗', 'Lock this user?')} placement="topLeft" onConfirm={() => userStatus('LOCKED')}>
        <a href="#">{t('锁定', 'Lock')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('您确定要冻结该用户吗', 'Suspend this user?')} placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">{t('冻结', 'Suspend')}</a>
      </Popconfirm>
    </>)
  }

  const renderInactive = () => {
    return (<>
      <Popconfirm title={t('您确定要激活该用户吗', 'Activate this user?')} placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
        <a href="#">{t('激活', 'Activate')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('您确定要冻结该用户吗', 'Suspend this user?')} placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">{t('冻结', 'Suspend')}</a>
      </Popconfirm>
    </>)
  }
  const renderLocked = () => {
    return (<>
      <Popconfirm title={t('您确定要该用户恢复正常吗', 'Restore this user to active status?')} placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
        <a href="#">{t('恢复正常', 'Restore')}</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title={t('您确定要冻结该用户吗', 'Suspend this user?')} placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">{t('冻结', 'Suspend')}</a>
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
        <Popconfirm title={t('您确定要该用户恢复正常吗', 'Restore this user to active status?')} placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
          <a href="#">{t('恢复正常', 'Restore')}</a>
        </Popconfirm>
        <Divider type="vertical"/>
        <Popconfirm title={t('您确定要锁定该用户吗', 'Lock this user?')} placement="topLeft" onConfirm={() => userStatus('LOCKED')}>
          <a href="#">{t('锁定', 'Lock')}</a>
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
      title: t('姓名', 'Name'),
      dataIndex: 'name',
      width: 120,
      fixed: 'left',
    },
    {
      title: t('头像', 'Avatar'),
      hideInSearch: true,
      width: 80,
      render: (_, user) => (
          <Image title={user.name} src={user.avatar} width={68} alt={user.name} original={false}/>
      ),
    },
    {
      title: t('背景', 'Background'),
      dataIndex: 'background',
      hideInSearch: true,
      width: 180,
      render: (_, user) => (
          <Image src={user.background} height="68" alt={user.name} original={false}/>
      ),
    },
    {
      title: t('邮箱', 'Email'),
      dataIndex: 'email',
    },
    {
      title: t('介绍', 'Bio'),
      dataIndex: 'introduce',
      hideInSearch: true,
    },
    {
      title: t('个人站点', 'Website'),
      dataIndex: 'site',
      hideInSearch: true,
      width: 150,
      render: (_, user) => <a href={user.site} target='_blank'>{user.site}</a>,
    },
    {
      title: t('注册类型', 'Registration type'),
      dataIndex: 'type',
      hideInSearch: true,
      width: 80,
    },
    {
      title: t('是否提醒', 'Notifications'),
      dataIndex: 'notification',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{user.notification ? t('已设置', 'Enabled') : t('未设置', 'Disabled')}</span>,
    },
    {
      title: t('默认密码', 'Default password'),
      dataIndex: 'defaultPassword',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{user.defaultPassword ? t('还未更换', 'Unchanged') : t('已经更换', 'Changed')}</span>,
    },
    {
      title: t('状态', 'Status'),
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{getUserStatusDesc(user.status)}</span>,
    },
    {
      title: t('操作', 'Actions'),
      valueType: 'option',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateUser(record)
              setUpdateModalVisible(true)
            }}
            >{t('编辑', 'Edit')}</a>
            <Divider type="vertical"/>
            <Popconfirm title={t('您确定要删除该用户吗', 'Delete this user?')} placement="topLeft" onConfirm={() => remove(record)}>
              <a href="#">{t('删除', 'Delete')}</a>
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
            headerTitle={t('用户列表', 'Users')}
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
