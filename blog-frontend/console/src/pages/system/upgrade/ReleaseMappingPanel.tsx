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
  return <>
    <div className="upgrade-toolbar">
      <span className="upgrade-secondary">映射在切换版本后发布为指向 current/&lt;source&gt; 的符号链接，回滚时自动指回旧版本。</span>
      <Space>
        <Button icon={<ReloadOutlined/>}
                onClick={() => onReload().catch(() => message.error('重新载入映射失败'))}>重新载入</Button>
        <Button type="primary" icon={<SaveOutlined/>} loading={saving}
                disabled={!loaded || !settings?.ready || !updaterReady}
                onClick={onSave}>保存映射</Button>
      </Space>
    </div>
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type={settings?.ready ? 'info' : 'warning'} showIcon
             message={settings?.ready ? '映射目标受服务器白名单限制' : '映射目标根目录配置未就绪'}
             description={settings?.ready
                 ? '目标必须位于服务器白名单目录内；已存在的非空目录不会被覆盖，发布失败会终止本次升级。'
                 : settings?.message || '正在加载映射配置'}/>
      <div>
        <span style={{ marginRight: 8 }}>允许的目标根目录：</span>
        {settings?.roots?.length
            ? settings.roots.map(root => <Tag key={root}>{root}</Tag>)
            : <Tag color="warning">未配置</Tag>}
      </div>
      <Form form={form} layout="vertical" disabled={!loaded || saving}>
        <Form.List name="mappings">
          {(fields, { remove }) => <Space direction="vertical" size="small" style={{ width: '100%' }}>
            {fields.map(field => <Row key={field.key} gutter={12} align="top">
              <Col xs={24} md={5}>
                <Form.Item name={[field.name, 'name']} label="名称"
                           rules={[
                             { required: true, message: '请输入名称' },
                             { pattern: /^[a-z][a-z0-9-]{0,31}$/, message: '小写字母开头，仅限小写字母、数字和短横线' },
                           ]}>
                  <Input placeholder="console" maxLength={32}/>
                </Form.Item>
              </Col>
              <Col xs={24} md={3}>
                <Form.Item name={[field.name, 'mode']} label="方式" rules={[{ required: true }]}>
                  <Select options={[{ value: 'SYMLINK', label: '符号链接' }]}/>
                </Form.Item>
              </Col>
              <Col xs={24} md={5}>
                <Form.Item name={[field.name, 'source']} label="发行源"
                           rules={[
                             { required: true, message: '请输入发行源' },
                             {
                               pattern: /^(console|desktop)(\/[A-Za-z0-9_@+.,-]+)*$/,
                               message: '只能是 console、desktop 或其子路径'
                             },
                           ]}>
                  <Input placeholder="console"/>
                </Form.Item>
              </Col>
              <Col xs={24} md={9}>
                <Form.Item name={[field.name, 'target']} label="Nginx 目标路径"
                           rules={[
                             { required: true, message: '请输入目标路径' },
                             { pattern: /^\/[^\t\n\r]+$/, message: '请输入规范化的绝对路径' },
                           ]}>
                  <Input placeholder="/var/www/today-blog/console"/>
                </Form.Item>
              </Col>
              <Col xs={24} md={2}>
                <Form.Item label=" ">
                  <Button danger type="text" icon={<MinusCircleOutlined/>}
                          onClick={() => remove(field.name)}/>
                </Form.Item>
              </Col>
            </Row>)}
            {!fields.length && <Alert type="info" showIcon message="尚未配置映射，前端页面需要由 nginx 直接引用版本目录"/>}
            <Button type="dashed" block icon={<PlusOutlined/>} onClick={onAdd}>添加映射</Button>
          </Space>}
        </Form.List>
      </Form>
      <span className="upgrade-secondary">映射由管理端保存到数据库；启动升级任务时会快照，之后修改不影响已提交任务。</span>
    </Space>
  </>;
}
