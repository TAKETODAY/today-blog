import React from 'react';
import {
  Alert,
  Button,
  Col,
  Form,
  Input,
  message,
  Row,
  Select,
  Space,
  Tag
} from 'antd';
import type { FormInstance } from 'antd';
import { MinusCircleOutlined, PlusOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import type { MappingSettings, ReleaseMapping } from './types';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  form: FormInstance<{ mappings: ReleaseMapping[] }>;
  settings?: MappingSettings;
  loaded: boolean;
  saving: boolean;
  updaterReady?: boolean;
  onSave: () => void;
  onReload: () => Promise<void>;
  onAdd: () => void;
}

export default function ReleaseMappingPanel({
  form, settings, loaded, saving, updaterReady, onSave, onReload, onAdd,
}: Props) {
  const t = useSiteTranslation();
  return <>
    <div className="upgrade-toolbar">
      <span className="upgrade-secondary">{t('upgrade.mappingsArePublishedAsSymlinks')}</span>
      <Space>
        <Button icon={<ReloadOutlined/>}
                onClick={() => onReload().catch(() => message.error(t('upgrade.couldNotReloadMappings')))}>{t('upgrade.reload')}</Button>
        <Button type="primary" icon={<SaveOutlined/>} loading={saving}
                disabled={!loaded || !settings?.ready || !updaterReady}
                onClick={onSave}>{t('upgrade.saveMappings')}</Button>
      </Space>
    </div>
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type={settings?.ready ? 'info' : 'warning'} showIcon
             message={settings?.ready ? t('upgrade.theServerRestrictsMappingTargets') : t('upgrade.mappingTargetDirectoriesAreNot')}
             description={settings?.ready
                 ? t('upgrade.targetsMustBeInsideAllowed')
                 : settings?.message || t('upgrade.loadingMappings')}/>
      <div>
        <span style={{ marginRight: 8 }}>{t('upgrade.allowedTargetDirectories')}</span>
        {settings?.roots?.length
            ? settings.roots.map(root => <Tag key={root}>{root}</Tag>)
            : <Tag color="warning">{t('upgrade.notConfigured')}</Tag>}
      </div>
      <Form form={form} layout="vertical" disabled={!loaded || saving}>
        <Form.List name="mappings">
          {(fields, { remove }) => <Space direction="vertical" size="small" style={{ width: '100%' }}>
            {fields.map(field => <Row key={field.key} gutter={12} align="top">
              <Col xs={24} md={5}>
                <Form.Item name={[field.name, 'name']} label={t('upgrade.name')}
                           rules={[
                             { required: true, message: t('upgrade.enterAName') },
                             { pattern: /^[a-z][a-z0-9-]{0,31}$/, message: t('upgrade.startWithALowercaseLetter') },
                           ]}>
                  <Input placeholder="console" maxLength={32}/>
                </Form.Item>
              </Col>
              <Col xs={24} md={3}>
                <Form.Item name={[field.name, 'mode']} label={t('upgrade.mode')} rules={[{ required: true }]}>
                  <Select options={[{ value: 'SYMLINK', label: t('upgrade.symlink') }]}/>
                </Form.Item>
              </Col>
              <Col xs={24} md={5}>
                <Form.Item name={[field.name, 'source']} label={t('upgrade.releaseSource')}
                           rules={[
                             { required: true, message: t('upgrade.enterAReleaseSource') },
                             {
                               pattern: /^(console|desktop)(\/[A-Za-z0-9_@+.,-]+)*$/,
                               message: t('upgrade.useConsoleDesktopOrA')
                             },
                           ]}>
                  <Input placeholder="console"/>
                </Form.Item>
              </Col>
              <Col xs={24} md={9}>
                <Form.Item name={[field.name, 'target']} label={t('upgrade.nginxTargetPath')}
                           rules={[
                             { required: true, message: t('upgrade.enterATargetPath') },
                             { pattern: /^\/[^\t\n\r]+$/, message: t('upgrade.enterANormalizedAbsolutePath') },
                           ]}>
                  <Input placeholder="/var/www/today-blog/console"/>
                </Form.Item>
              </Col>
              <Col xs={24} md={2}>
                <Form.Item label=" ">
                  <Button danger type="text" icon={<MinusCircleOutlined/>} aria-label={t('upgrade.removeMapping')}
                          onClick={() => remove(field.name)}/>
                </Form.Item>
              </Col>
            </Row>)}
            {!fields.length && <Alert type="info" showIcon message={t('upgrade.noMappingsConfiguredPointNginx')}/>}
            <Button type="dashed" block icon={<PlusOutlined/>} onClick={onAdd}>{t('upgrade.addMapping')}</Button>
          </Space>}
        </Form.List>
      </Form>
      <span className="upgrade-secondary">{t('upgrade.mappingsAreStoredInThe')}</span>
    </Space>
  </>;
}
