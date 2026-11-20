import React from 'react';
import { Modal, Tabs } from 'antd';
import type { UpgradeSettingsController } from './useUpgradeSettings';
import UpgradeSettingsPanel from './UpgradeSettingsPanel';
import ReleaseMappingPanel from './ReleaseMappingPanel';
import { useSiteTranslation } from '@/utils/use-site-translation';

interface Props {
  open: boolean;
  activeTab: string;
  loaded: boolean;
  updaterReady?: boolean;
  settings: UpgradeSettingsController;
  onTabChange: (key: string) => void;
  onClose: () => void;
}

export default function UpgradeConfigModal({
  open, activeTab, loaded, updaterReady, settings, onTabChange, onClose,
}: Props) {
  const t = useSiteTranslation();
  return <Modal className="upgrade-config-modal" title={t('upgrade.upgradeConfiguration')} open={open} forceRender footer={null}
                width={760} style={{ maxWidth: 'calc(100vw - 32px)', top: 40 }}
                bodyStyle={{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto' }}
                onCancel={onClose}>
    <Tabs activeKey={activeTab} onChange={onTabChange} items={[
      {
        key: 'settings',
        label: t('upgrade.upgradeSettings'),
        forceRender: true,
        children: <UpgradeSettingsPanel
            form={settings.settingsForm}
            loaded={loaded}
            saving={settings.saving}
            updaterReady={updaterReady}
            onSave={settings.saveSettings}/>,
      },
      {
        key: 'mappings',
        label: t('upgrade.fileMappings'),
        forceRender: true,
        children: <ReleaseMappingPanel
            form={settings.mappingForm}
            settings={settings.mappingSettings}
            loaded={loaded}
            saving={settings.mappingSaving}
            updaterReady={updaterReady}
            onSave={settings.saveMappings}
            onReload={settings.reloadMappings}
            onAdd={settings.addMapping}/>,
      },
    ]}/>
  </Modal>;
}
