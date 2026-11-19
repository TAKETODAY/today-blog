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
      <span className="upgrade-secondary">{t('映射在切换版本后发布为指向 current/<source> 的符号链接，回滚时自动指回旧版本。', 'Mappings are published as symlinks to current/<source> after switching releases. Rollback points them back to the previous release.')}</span>
      <Space>
        <Button icon={<ReloadOutlined/>}
                onClick={() => onReload().catch(() => message.error(t('重新载入映射失败', 'Could not reload mappings')))}>{t('重新载入', 'Reload')}</Button>
        <Button type="primary" icon={<SaveOutlined/>} loading={saving}
                disabled={!loaded || !settings?.ready || !updaterReady}
                onClick={onSave}>{t('保存映射', 'Save mappings')}</Button>
      </Space>
    </div>
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type={settings?.ready ? 'info' : 'warning'} showIcon
             message={settings?.ready ? t('映射目标受服务器白名单限制', 'The server restricts mapping targets') : t('映射目标根目录配置未就绪', 'Mapping target directories are not ready')}
             description={settings?.ready
                 ? t('目标必须位于服务器白名单目录内；已存在的非空目录不会被覆盖，发布失败会终止本次升级。', 'Targets must be inside allowed directories. Existing non-empty directories are never overwritten. A publishing failure aborts the upgrade.')
                 : settings?.message || t('正在加载映射配置', 'Loading mappings')}/>
      <div>
        <span style={{ marginRight: 8 }}>{t('允许的目标根目录：', 'Allowed target directories:')}</span>
        {settings?.roots?.length
            ? settings.roots.map(root => <Tag key={root}>{root}</Tag>)
            : <Tag color="warning">{t('未配置', 'Not configured')}</Tag>}
      </div>
      <Form form={form} layout="vertical" disabled={!loaded || saving}>
        <Form.List name="mappings">
          {(fields, { remove }) => <Space direction="vertical" size="small" style={{ width: '100%' }}>
            {fields.map(field => <Row key={field.key} gutter={12} align="top">
              <Col xs={24} md={5}>
                <Form.Item name={[field.name, 'name']} label={t('名称', 'Name')}
                           rules={[
                             { required: true, message: t('请输入名称', 'Enter a name') },
                             { pattern: /^[a-z][a-z0-9-]{0,31}$/, message: t('小写字母开头，仅限小写字母、数字和短横线', 'Start with a lowercase letter; use only lowercase letters, digits and hyphens') },
                           ]}>
                  <Input placeholder="console" maxLength={32}/>
                </Form.Item>
              </Col>
              <Col xs={24} md={3}>
                <Form.Item name={[field.name, 'mode']} label={t('方式', 'Mode')} rules={[{ required: true }]}>
                  <Select options={[{ value: 'SYMLINK', label: t('符号链接', 'Symlink') }]}/>
                </Form.Item>
              </Col>
              <Col xs={24} md={5}>
                <Form.Item name={[field.name, 'source']} label={t('发行源', 'Release source')}
                           rules={[
                             { required: true, message: t('请输入发行源', 'Enter a release source') },
                             {
                               pattern: /^(console|desktop)(\/[A-Za-z0-9_@+.,-]+)*$/,
                               message: t('只能是 console、desktop 或其子路径', 'Use console, desktop, or a path under either directory')
                             },
                           ]}>
                  <Input placeholder="console"/>
                </Form.Item>
              </Col>
              <Col xs={24} md={9}>
                <Form.Item name={[field.name, 'target']} label={t('Nginx 目标路径', 'Nginx target path')}
                           rules={[
                             { required: true, message: t('请输入目标路径', 'Enter a target path') },
                             { pattern: /^\/[^\t\n\r]+$/, message: t('请输入规范化的绝对路径', 'Enter a normalized absolute path') },
                           ]}>
                  <Input placeholder="/var/www/today-blog/console"/>
                </Form.Item>
              </Col>
              <Col xs={24} md={2}>
                <Form.Item label=" ">
                  <Button danger type="text" icon={<MinusCircleOutlined/>} aria-label={t('移除映射', 'Remove mapping')}
                          onClick={() => remove(field.name)}/>
                </Form.Item>
              </Col>
            </Row>)}
            {!fields.length && <Alert type="info" showIcon message={t('尚未配置映射，前端页面需要由 nginx 直接引用版本目录', 'No mappings configured. Point Nginx directly to the release directory to serve frontend assets.')}/>}
            <Button type="dashed" block icon={<PlusOutlined/>} onClick={onAdd}>{t('添加映射', 'Add mapping')}</Button>
          </Space>}
        </Form.List>
      </Form>
      <span className="upgrade-secondary">{t('映射由管理端保存到数据库；启动升级任务时会快照，之后修改不影响已提交任务。', 'Mappings are stored in the database and copied when an upgrade starts. Changes do not affect submitted tasks.')}</span>
    </Space>
  </>;
}
