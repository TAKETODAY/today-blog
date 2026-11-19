import { useCallback, useState } from 'react';
import { Form, message } from 'antd';
import type { FormInstance } from 'antd';
import { http } from '@/utils/http';
import { siteText as t } from '@/utils/site-i18n';
import type { MappingSettings, Options, ReleaseMapping } from './types';
import { parseOptions, requestOptions } from './utils';

export interface UpgradeSettingsController {
  settingsForm: FormInstance<Options>;
  mappingForm: FormInstance<{ mappings: ReleaseMapping[] }>;
  mappingSettings?: MappingSettings;
  saving: boolean;
  mappingSaving: boolean;
  apply: (options: Options, mappings: MappingSettings) => void;
  saveSettings: () => Promise<void>;
  saveMappings: () => Promise<void>;
  reloadMappings: () => Promise<void>;
  addMapping: () => void;
}

export function useUpgradeSettings(): UpgradeSettingsController {
  const [settingsForm] = Form.useForm<Options>();
  const [mappingForm] = Form.useForm<{ mappings: ReleaseMapping[] }>();
  const [mappingSettings, setMappingSettings] = useState<MappingSettings>();
  const [saving, setSaving] = useState(false);
  const [mappingSaving, setMappingSaving] = useState(false);

  const apply = useCallback((options: Options, mappings: MappingSettings) => {
    settingsForm.setFieldsValue(options);
    setMappingSettings(mappings);
    mappingForm.setFieldsValue({ mappings: mappings.mappings || [] });
  }, [settingsForm, mappingForm]);

  async function saveSettings() {
    const options = await settingsForm.validateFields();
    setSaving(true);
    try {
      const response = await http.put('/api/console/maintenance/upgrade/settings', requestOptions(options));
      settingsForm.setFieldsValue(parseOptions(response.data));
      message.success(t('upgrade.defaultsSavedCurrentUpgradeParameters'));
    }
    finally {
      setSaving(false);
    }
  }

  async function saveMappings() {
    const values = await mappingForm.validateFields();
    setMappingSaving(true);
    try {
      const response = await http.put('/api/console/maintenance/upgrade/mappings', {
        mappings: (values.mappings || []).map(mapping => ({
          ...mapping,
          mode: mapping.mode || 'SYMLINK',
          target: (mapping.target || '').trim(),
        })),
      });
      setMappingSettings(response.data);
      mappingForm.setFieldsValue({ mappings: response.data.mappings || [] });
      message.success(t('upgrade.fileMappingsSavedSubmittedTasks'));
    }
    finally {
      setMappingSaving(false);
    }
  }

  async function reloadMappings() {
    const response = await http.get('/api/console/maintenance/upgrade/mappings');
    setMappingSettings(response.data);
    mappingForm.setFieldsValue({ mappings: response.data.mappings || [] });
  }

  function addMapping() {
    const root = mappingSettings?.roots?.[0] || '/var/www/today-blog';
    const mappings = mappingForm.getFieldValue('mappings') || [];
    const used = new Set(mappings.map((mapping: ReleaseMapping) => mapping.name));
    const name = used.has('console') ? (used.has('desktop') ? `mapping-${mappings.length + 1}` : 'desktop') : 'console';
    mappingForm.setFieldsValue({
      mappings: [...mappings, {
        name,
        mode: 'SYMLINK',
        source: name === 'desktop' ? 'desktop' : 'console',
        target: `${root}/${name}`,
      }],
    });
  }

  return {
    settingsForm, mappingForm, mappingSettings, saving, mappingSaving,
    apply, saveSettings, saveMappings, reloadMappings, addMapping,
  };
}
