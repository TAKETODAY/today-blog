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
import { format, isNotEmpty } from "@/utils";
import { useSiteTranslation } from '@/utils/use-site-translation';

import { LoggingItem } from "./data.d";
import { deleteById, queryLogging, } from "./service";


/**
 *  删除节点
 * @param logging
 */
const handleRemove = async (logging: LoggingItem, t: ReturnType<typeof useSiteTranslation>) => {
  const hide = message.loading(t('logs.deleting', { title: logging.title }))
  try {
    await doRemove(logging.id)
    hide()
    message.success(t('logs.deleted'), 1)
    return true
  }
  catch (error) {
    hide()
    message.error(t('logs.couldNotDeletePleaseTry'))
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
  const t = useSiteTranslation();
  const actionRef = useRef<ActionType>()

  const reload = async () => {
    await actionRef.current?.reload()
  }

  const remove = async (record: LoggingItem) => {
    await handleRemove(record, t)
    await reload()
  }

  const columns: ProColumns<LoggingItem>[] = [
    {
      title: t('logs.type'),
      width: 60,
      dataIndex: 'type',
      hideInSearch: true,
      render: (_, record) => (
          <Tag color={getColor(record.type)}>{record.type}</Tag>
      ),
    },
    {
      title: t('logs.title'),
      dataIndex: 'title',
      width: 100,
      hideInSearch: true
    },
    {
      title: t('logs.details'),
      hideInSearch: true,
      render: (_, record) => (
          <span dangerouslySetInnerHTML={{ __html: record.content }}/>
      ),
      width: 400,
    },
    {
      title: t('logs.user'),
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
      title: t('logs.timestamp'),
      dataIndex: 'invokeAt',
      sorter: true,
      valueType: 'dateTimeRange',
      width: 180,
      render: (_, record) => (
          <>
            {format(record.invokeAt)}
          </>
      ),
    },
    {
      title: t('logs.actions'),
      valueType: 'option',
      width: 50,
      fixed: 'right',
      render: (_, record) => (
          <>
            <Popconfirm title={t('logs.deleteThisLogEntry')} placement="topLeft" onConfirm={() => remove(record)}>
              <a href="#">{t('logs.delete')}</a>
            </Popconfirm>
          </>
      ),
    },
  ]

  async function removeSelected(selected: (number | string)[]) {
    if (isNotEmpty(selected)) {
      const hide = message.loading(t('logs.deletingSelectedLogEntries'))
      try {
        await selected.forEach(doRemove)
        message.success(t('logs.deleted'), 1)
      }
      catch (error) {
        message.error(t('logs.couldNotDeletePleaseTry'))
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
            headerTitle={t('logs.systemLogs')}
            actionRef={actionRef}
            request={queryLogging}
            columns={columns}
            rowSelection={{}}
            tableAlertRender={({ selectedRowKeys, selectedRows, onCleanSelected }) => (
                <Space size={24}>
                  <span>{t('logs.selectionCount', { count: selectedRowKeys.length })}
                    <a style={{ marginLeft: 8 }} onClick={onCleanSelected}>
                      {t('logs.clearSelection')}
                    </a>
                  </span>
                  <Popconfirm title={t('logs.deleteTheSelectedLogEntries')} onConfirm={() => removeSelected(selectedRowKeys)}>
                    <a>{t('logs.deleteSelected')}</a>
                  </Popconfirm>
                </Space>
            )}
        />

      </PageContainer>
  )
}
