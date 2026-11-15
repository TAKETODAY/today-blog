import React from 'react';
import { Modal, Tabs } from 'antd';
import type { UpgradeSettingsController } from './useUpgradeSettings';
import UpgradeSettingsPanel from './UpgradeSettingsPanel';
import ReleaseMappingPanel from './ReleaseMappingPanel';

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
  return <Modal className="upgrade-config-modal" title="升级配置" open={open} forceRender footer={null}
                width={760} style={{ maxWidth: 'calc(100vw - 32px)', top: 40 }}
                bodyStyle={{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto' }}
                onCancel={onClose}>
    <Tabs activeKey={activeTab} onChange={onTabChange} items={[
      {
        key: 'settings',
        label: '升级设置',
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
        label: '文件映射',
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
