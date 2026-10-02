import React, { useCallback, useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Space, Switch, Tag, Tooltip } from 'antd';
import { CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons';
import { query } from "./service";
import OptionEditModal from "./OptionEditModal";
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { format } from "@/utils";
import { OptionItem } from './types';


const renderConfigValue = (record: OptionItem): React.ReactNode => {
  if (record.valueType === 'bool') {
    return (
        <Switch checked={record.value === 'true'} checkedChildren={<CheckOutlined/>} unCheckedChildren={<CloseOutlined/>}/>
    );
  }
  return <span>{record.value || <Tag>N/A</Tag>}</span>;
}

const ConfigManagement: React.FC = () => {
  const actionRef = useRef<ActionType>()
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentOption, setCurrentOption] = useState<OptionItem | null>(null);

  const reload = useCallback(async () => {
    await actionRef.current?.reload()
  }, [])

  const handleEdit = (record: OptionItem) => {
    setCurrentOption(record);
    setEditModalVisible(true);
  }

  const columns: ProColumns<OptionItem>[] = [
    {
      fixed: 'left',
      title: '名称',
      key: 'name',
      width: 280,
      ellipsis: true,
      render: (_, record) => (
          <Tooltip title={record.name} key={record.name}>
            <span style={{ fontFamily: 'monospace', fontSize: 13 }}>{record.name}</span>
          </Tooltip>
      ),
    },
    {
      title: '配置值',
      key: 'value',
      width: 280,
      hideInSearch: true,
      render: (_, record) => {
        return renderConfigValue(record)
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 280,
      hideInSearch: true,
      render: (_, record) => <span>{record.description || <Tag>N/A</Tag>}</span>,
    },
    {
      title: '公开状态',
      width: 100,
      align: 'center',
      hideInSearch: true,
      render: (_, record) => (record.open ? <Tag color="success">公开</Tag> : <Tag>仅内部使用</Tag>)
    },
    {
      title: '更新时间',
      width: 180,
      hideInSearch: true,
      render: (_, record: OptionItem) => (record.updateAt ? format(record.updateAt)
          : <Tag>N/A</Tag>)
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      align: 'center',
      hideInSearch: true,
      render: (_, record) => (
          <Space size="small">
            <a onClick={() => handleEdit(record)}><EditOutlined/>编辑</a>
          </Space>
      ),
    },
  ];

  return (
      <PageContainer header={{ title: '系统配置管理' }}>
        <ProTable<OptionItem>
            rowKey="name"
            headerTitle="系统配置列表"
            actionRef={actionRef}
            request={query}
            columns={columns}
            scroll={{ x: 1200 }}
        />

        <OptionEditModal option={currentOption} visible={editModalVisible} onSuccess={() => {
          setEditModalVisible(false)
          return reload()
        }} onCancel={() => setEditModalVisible(false)}/>

      </PageContainer>
  );
};

export default ConfigManagement;