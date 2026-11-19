import React, { useCallback, useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Space, Switch, Tag, Tooltip } from 'antd';
import { CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons';
import { query } from "./service";
import OptionEditModal from "./OptionEditModal";
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table'
import { format } from "@/utils";
import { OptionItem } from './types';
import { applySiteOptions } from '@/utils/site-display';
import { useSiteTranslation } from '@/utils/use-site-translation';
import { SITE_LOCALE, SITE_TIME_ZONE } from '../../../../../common/site-settings';


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
  const t = useSiteTranslation();

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
      title: t('名称', 'Name'),
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
      title: t('配置值', 'Value'),
      key: 'value',
      width: 280,
      hideInSearch: true,
      render: (_, record) => {
        return renderConfigValue(record)
      },
    },
    {
      title: t('描述', 'Description'),
      dataIndex: 'description',
      key: 'description',
      width: 280,
      hideInSearch: true,
      render: (_, record) => {
        const description = record.name === SITE_LOCALE ? t('站点默认语言（zh-CN 或 en-US）', 'Default site language (zh-CN or en-US)')
          : record.name === SITE_TIME_ZONE ? t('站点默认时区（IANA 标识）', 'Default display time zone (IANA identifier)')
          : record.description;
        return description || <Tag>N/A</Tag>;
      },
    },
    {
      title: t('公开状态', 'Visibility'),
      width: 100,
      align: 'center',
      hideInSearch: true,
      render: (_, record) => (record.open ? <Tag color="success">{t('公开', 'Public')}</Tag> : <Tag>{t('仅内部使用', 'Internal')}</Tag>)
    },
    {
      title: t('更新时间', 'Updated'),
      width: 180,
      hideInSearch: true,
      render: (_, record: OptionItem) => (record.updateAt ? format(record.updateAt)
          : <Tag>N/A</Tag>)
    },
    {
      title: t('操作', 'Actions'),
      key: 'action',
      width: 150,
      align: 'center',
      hideInSearch: true,
      render: (_, record) => (
          <Space size="small">
            <a onClick={() => handleEdit(record)}><EditOutlined/>{t('编辑', 'Edit')}</a>
          </Space>
      ),
    },
  ];

  return (
      <PageContainer header={{ title: t('系统配置管理', 'System settings') }}>
        <ProTable<OptionItem>
            rowKey="name"
            headerTitle={t('系统配置列表', 'Site options')}
            actionRef={actionRef}
            request={query}
            columns={columns}
            scroll={{ x: 1200 }}
        />

        <OptionEditModal option={currentOption} visible={editModalVisible} onSuccess={({ name, value }) => {
          if (name === SITE_LOCALE || name === SITE_TIME_ZONE) {
            applySiteOptions({ [name]: value });
          }
          setEditModalVisible(false);
          void reload();
        }} onCancel={() => setEditModalVisible(false)}/>

      </PageContainer>
  );
};

export default ConfigManagement;
