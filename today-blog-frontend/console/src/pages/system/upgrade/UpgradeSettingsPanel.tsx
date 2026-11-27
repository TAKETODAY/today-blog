import React from 'react';
import { Button, Form } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import type { FormInstance } from 'antd';
import type { Options } from './types';
import OptionFields from './OptionFields';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  form: FormInstance<Options>;
  loaded: boolean;
  saving: boolean;
  updaterReady?: boolean;
  onSave: () => void;
}

export default function UpgradeSettingsPanel({ form, loaded, saving, updaterReady, onSave }: Props) {
  const t = useSiteTranslation();
  return <>
    <div className="upgrade-toolbar">
      <span className="upgrade-secondary">{t('upgrade.defaultsApplyTheNextTime')}</span>
      <Button type="primary" icon={<SaveOutlined/>} onClick={onSave} loading={saving}
              disabled={!loaded || !updaterReady}>{t('upgrade.saveDefaults')}</Button>
    </div>
    <Form form={form} layout="vertical" disabled={!loaded || saving}>
      <OptionFields/>
    </Form>
  </>;
}
