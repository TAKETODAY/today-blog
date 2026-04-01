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

/**
 * 更新节点
 * @param user
 */
const handleUpdate = async (user: UserItem) => {
  const hide = message.loading(`正在更新: ${user.name}`)
  try {
    await update(user)
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
 * @param user
 */
const handleRemove = async (user: UserItem) => {
  const hide = message.loading(`正在删除: ${user.name}`)
  try {
    await deleteUser(user)
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


const renderStatusMenu = (user: UserItem, reload: Function) => {
  const userStatus = async (status: string) => {
    const hide = message.loading(`正在切换状态: ${user.name}`)
    try {
      await toggleStatus(user.id, status)
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

  const renderNormal = () => {
    return (<>
      <Popconfirm title="您确定要锁定该用户吗" placement="topLeft" onConfirm={() => userStatus('LOCKED')}>
        <a href="#">锁定</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要冻结该用户吗" placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">冻结</a>
      </Popconfirm>
    </>)
  }

  const renderInactive = () => {
    return (<>
      <Popconfirm title="您确定要激活该用户吗" placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
        <a href="#">激活</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要冻结该用户吗" placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">冻结</a>
      </Popconfirm>
    </>)
  }
  const renderLocked = () => {
    return (<>
      <Popconfirm title="您确定要该用户恢复正常吗" placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
        <a href="#">恢复正常</a>
      </Popconfirm>
      <Divider type="vertical"/>
      <Popconfirm title="您确定要冻结该用户吗" placement="topLeft" onConfirm={() => userStatus('RECYCLE')}>
        <a href="#">冻结</a>
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
        <Popconfirm title="您确定要该用户恢复正常吗" placement="topLeft" onConfirm={() => userStatus('NORMAL')}>
          <a href="#">恢复正常</a>
        </Popconfirm>
        <Divider type="vertical"/>
        <Popconfirm title="您确定要锁定该用户吗" placement="topLeft" onConfirm={() => userStatus('LOCKED')}>
          <a href="#">锁定</a>
        </Popconfirm>
      </>
  )
}

export default () => {
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
      title: '姓名',
      dataIndex: 'name',
      width: 120,
      fixed: 'left',
    },
    {
      title: '头像',
      hideInSearch: true,
      width: 80,
      render: (_, user) => (
          <Image title={user.name} src={user.avatar} width={68} alt={user.name} original={false}/>
      ),
    },
    {
      title: '背景',
      dataIndex: 'background',
      hideInSearch: true,
      width: 180,
      render: (_, user) => (
          <Image src={user.background} height="68" alt={user.name} original={false}/>
      ),
    },
    {
      title: '邮箱',
      dataIndex: 'email',
    },
    {
      title: '介绍',
      dataIndex: 'introduce',
      hideInSearch: true,
    },
    {
      title: '个人站点',
      dataIndex: 'site',
      hideInSearch: true,
      width: 150,
      render: (_, user) => <a href={user.site} target='_blank'>{user.site}</a>,
    },
    {
      title: '注册类型',
      dataIndex: 'type',
      hideInSearch: true,
      width: 80,
    },
    {
      title: '是否提醒',
      dataIndex: 'notification',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{user.notification ? '已设置' : '未设置'}</span>,
    },
    {
      title: '默认密码',
      dataIndex: 'defaultPassword',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{user.defaultPassword ? '还未更换' : '已经更换'}</span>,
    },
    {
      title: '状态',
      hideInSearch: true,
      width: 80,
      render: (_, user) => <span>{getUserStatusDesc(user.status)}</span>,
    },
    {
      title: '操作',
      valueType: 'option',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
          <>
            <a onClick={() => {
              setUpdateUser(record)
              setUpdateModalVisible(true)
            }}
            > 编辑</a>
            <Divider type="vertical"/>
            <Popconfirm title="您确定要删除该用户吗" placement="topLeft" onConfirm={() => remove(record)}>
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
        <ProTable<UserItem>
            rowKey="id"
            headerTitle="博客文章列表"
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
