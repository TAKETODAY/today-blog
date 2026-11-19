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
      title: t('options.name'),
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
      title: t('options.value'),
      key: 'value',
      width: 280,
      hideInSearch: true,
      render: (_, record) => {
        return renderConfigValue(record)
      },
    },
    {
      title: t('options.description'),
      dataIndex: 'description',
      key: 'description',
      width: 280,
      hideInSearch: true,
      render: (_, record) => {
        const description = record.name === SITE_LOCALE ? t('options.defaultSiteLanguageZhCN')
          : record.name === SITE_TIME_ZONE ? t('options.defaultDisplayTimeZoneIANA')
          : record.description;
        return description || <Tag>N/A</Tag>;
      },
    },
    {
      title: t('options.visibility'),
      width: 100,
      align: 'center',
      hideInSearch: true,
      render: (_, record) => (record.open ? <Tag color="success">{t('options.public')}</Tag> : <Tag>{t('options.internal')}</Tag>)
    },
    {
      title: t('options.updated'),
      width: 180,
      hideInSearch: true,
      render: (_, record: OptionItem) => (record.updateAt ? format(record.updateAt)
          : <Tag>N/A</Tag>)
    },
    {
      title: t('options.actions'),
      key: 'action',
      width: 150,
      align: 'center',
      hideInSearch: true,
      render: (_, record) => (
          <Space size="small">
            <a onClick={() => handleEdit(record)}><EditOutlined/>{t('options.edit')}</a>
          </Space>
      ),
    },
  ];

  return (
      <PageContainer header={{ title: t('options.systemSettings') }}>
        <ProTable<OptionItem>
            rowKey="name"
            headerTitle={t('options.siteOptions')}
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
