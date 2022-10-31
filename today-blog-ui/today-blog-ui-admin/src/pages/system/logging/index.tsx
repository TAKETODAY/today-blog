import React, { useRef } from 'react';
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

export default () => {
  const actionRef = useRef<ActionType>()

  const reload = () => {
    actionRef.current?.reload()
  }

  const remove = async (record: LoggingItem) => {
    await handleRemove(record)
    reload()
  }

  const columns: ProColumns<LoggingItem>[] = [
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
    },
    {
      title: '用户',
      dataIndex: 'user',
      hideInSearch: true
    },
    {
      title: '类型',
      dataIndex: 'type',
      hideInSearch: true,
      render: (_, record) => (
          <Tag color={record.type}>{record.type}</Tag>
      ),
    },
    {
      title: '结果',
      dataIndex: 'result',
      hideInSearch: true
    },
    {
      title: 'IP',
      dataIndex: 'ip',
      hideInSearch: true,
      width: 200,
    },
    {
      title: '产生日期',
      dataIndex: 'id',
      sorter: true,
      valueType: 'dateTimeRange',
      width: 180,
      render: (_, record) => (
          <>
            {moment(record.id).format('lll')}
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
        reload()
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
