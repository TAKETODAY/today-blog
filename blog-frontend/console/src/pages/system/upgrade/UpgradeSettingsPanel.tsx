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
      <span className="upgrade-secondary">{t('默认设置仅用于以后载入页面时初始化参数，不影响已提交的任务；命令及数据库凭据由运维配置。',
        'Defaults apply the next time the page loads and do not affect submitted tasks. Configure commands and database credentials on the server.')}</span>
      <Button type="primary" icon={<SaveOutlined/>} onClick={onSave} loading={saving}
              disabled={!loaded || !updaterReady}>{t('保存默认设置', 'Save defaults')}</Button>
    </div>
    <Form form={form} layout="vertical" disabled={!loaded || saving}>
      <OptionFields/>
    </Form>
  </>;
}
