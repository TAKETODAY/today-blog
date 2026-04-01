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

import { useRef } from 'react';
import { message, Popconfirm, Space, Tag } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { isNotEmpty } from "@/utils";

import { LoggingItem } from "./data.d";
import { deleteById, queryLogging, } from "./service";
import moment from "moment";


/**
 *  删除节点
 * @param logging
 */
const handleRemove = async (logging: LoggingItem) => {
  const hide = message.loading(`正在删除: ${logging.title}`)
  try {
    await doRemove(logging.id)
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

const doRemove = async (id: (number | string)) => {
  await deleteById(id)
}

function getColor(type): string {
  switch (type) {
    case "INFO":
      return "default";
    case "WARN":
      return "warning";
    case "ERROR":
      return "error";
    case "SUCCESS":
      return "success";
    default:
      throw Error();
  }
}

export default () => {
  const actionRef = useRef<ActionType>()

  const reload = async () => {
    await actionRef.current?.reload()
  }

  const remove = async (record: LoggingItem) => {
    await handleRemove(record)
    await reload()
  }

  const columns: ProColumns<LoggingItem>[] = [
    {
      title: '类型',
      width: 60,
      dataIndex: 'type',
      hideInSearch: true,
      render: (_, record) => (
          <Tag color={getColor(record.type)}>{record.type}</Tag>
      ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      width: 100,
      hideInSearch: true
    },
    {
      title: '内容',
      hideInSearch: true,
      render: (_, record) => (
          <span dangerouslySetInnerHTML={{ __html: record.content }}/>
      ),
      width: 400,
    },
    {
      title: '用户',
      dataIndex: 'user',
      hideInSearch: true,
      width: 100,
    },
    {
      title: 'IP',
      dataIndex: 'ip',
      hideInSearch: true,
      width: 240,
      render: (_, record) => (<>
        {record.ip}/{record.ipCountry}/{record.ipProvince}/{record.ipCity}/{record.ipArea}/{record.ipIsp}
      </>),
    },
    {
      title: '产生日期',
      dataIndex: 'invokeAt',
      sorter: true,
      valueType: 'dateTimeRange',
      width: 180,
      render: (_, record) => (
          <>
            {moment(record.invokeAt).format('lll')}
          </>
      ),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 50,
      fixed: 'right',
      render: (_, record) => (
          <>
            <Popconfirm title="您确定要删除该条日志吗" placement="topLeft" onConfirm={() => remove(record)}>
              <a href="#">删除</a>
            </Popconfirm>
          </>
      ),
    },
  ]

  async function removeSelected(selected: (number | string)[]) {
    if (isNotEmpty(selected)) {
      const hide = message.loading(`正在批量删除`)
      try {
        await selected.forEach(doRemove)
        message.success('删除成功，即将刷新', 1)
      }
      catch (error) {
        message.error('删除失败，请重试')
      }
      finally {
        hide()
        await reload()
      }
    }
  }

  return (
      <PageContainer>
        <ProTable<LoggingItem>
            rowKey="id"
            scroll={{ x: 2000 }}
            headerTitle="系统日志列表"
            actionRef={actionRef}
            request={queryLogging}
            columns={columns}
            rowSelection={{}}
            tableAlertRender={({ selectedRowKeys, selectedRows, onCleanSelected }) => (
                <Space size={24}>
                  <span> 已选 {selectedRowKeys.length} 项
                    <a style={{ marginLeft: 8 }} onClick={onCleanSelected}>
                      取消选择
                    </a>
                  </span>
                  <Popconfirm title="确定要批量删除这些日志吗" onConfirm={() => removeSelected(selectedRowKeys)}>
                    <a>批量删除</a>
                  </Popconfirm>
                </Space>
            )}
        />

      </PageContainer>
  )
}
