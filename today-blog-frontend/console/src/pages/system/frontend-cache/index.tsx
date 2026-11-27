import React, { useEffect, useState } from 'react';
import { PageContainer } from '@ant-design/pro-layout';
import { Alert, Button, Card, Col, Form, Row, Select, Space, Spin, Typography, message } from 'antd';
import { http } from '@/utils';
import { useSiteTranslation } from '@/utils/use-site-translation';

const endpoint = '/api/console/frontend-cache';
const presetNames = ['inherit', 'no-store', 'no-cache', '5m', '1h', '1d', '1y'] as const;
const groups = [
  {
    name: 'console',
    fields: [['console.index', 'index'], ['console.assets', 'assets'], ['console.hashed-assets', 'hashedAssets']],
  },
  {
    name: 'desktop',
    fields: [['desktop.index', 'index'], ['desktop.assets', 'assets'], ['desktop.hashed-assets', 'hashedAssets']],
  },
  {
    name: 'upload',
    fields: [['upload', 'uploadPolicy']],
  },
] as const;

type Preset = typeof presetNames[number];
type CacheField = typeof groups[number]['fields'][number][0];
type Presets = Partial<Record<CacheField, Preset>>;

interface CacheSettings {
  embedded: boolean;
  presets: Presets;
  effective: Partial<Record<CacheField, string>>;
}

export default function FrontendCache() {
  const t = useSiteTranslation();
  const [settings, setSettings] = useState<CacheSettings>();
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadAttempt, setLoadAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    setLoadFailed(false);
    http.get(endpoint).then(({ data }) => {
      if (active) {
        setSettings(data);
      }
    }).catch(() => {
      if (active) {
        setLoadFailed(true);
      }
    });
    return () => {
      active = false;
    };
  }, [loadAttempt]);

  return (
    <PageContainer header={{ title: t('frontendCache.title') }}>
      {loadFailed && <Alert type="error" showIcon message={t('frontendCache.loadFailed')}
        action={<Button onClick={() => setLoadAttempt(attempt => attempt + 1)}>{t('frontendCache.retry')}</Button>}/>}
      {!settings && !loadFailed && <Spin/>}
      {settings && (settings.embedded
        ? <CacheForm settings={settings} onSaved={setSettings}/>
        : <Alert type="info" showIcon message={t('frontendCache.nginx')}/>)}
    </PageContainer>
  );
}

function CacheForm({ settings, onSaved }: { settings: CacheSettings; onSaved: (settings: CacheSettings) => void }) {
  const t = useSiteTranslation();
  const [saving, setSaving] = useState(false);
  const presetOptions = presetNames.map(value => ({ value, label: t(`frontendCache.preset.${value}`) }));
  const initialValues: Presets = {};
  for (const { fields } of groups) {
    for (const [key] of fields) {
      initialValues[key] = settings.presets[key] ?? 'inherit';
    }
  }

  async function save(presets: Presets) {
    setSaving(true);
    try {
      const { data } = await http.put(endpoint, presets);
      onSaved(data);
      message.success(t('options.saved'));
    }
    catch {
      // The shared HTTP handler displays the error; keep the selections for retry.
    }
    finally {
      setSaving(false);
    }
  }

  return (
    <Form layout="vertical" initialValues={initialValues} onFinish={save} disabled={saving}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert type="info" showIcon message={t('frontendCache.notice')}/>
        <Row gutter={[16, 16]}>
          {groups.map(({ name, fields }) => (
            <Col xs={24} md={12} key={name}>
              <Card title={t(`frontendCache.${name}`)}>
                {fields.map(([key, label]) => (
                  <Form.Item key={key} name={key} label={t(`frontendCache.${label}`)}
                    extra={<>{t('frontendCache.effective')}<Typography.Text code>{settings.effective[key]}</Typography.Text></>}>
                    <Select options={presetOptions}/>
                  </Form.Item>
                ))}
              </Card>
            </Col>
          ))}
        </Row>
        <Button type="primary" htmlType="submit" loading={saving}>{t('frontendCache.save')}</Button>
      </Space>
    </Form>
  );
}
