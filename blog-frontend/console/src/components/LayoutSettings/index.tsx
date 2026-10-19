import { useState } from 'react';
import { Button, message } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { SettingDrawer } from '@ant-design/pro-layout';
import { useModel } from '@umijs/max';
import { normalizeLayoutSettings, saveLayoutSettings } from '@/utils/layout-settings';
import styles from './index.less';

export default function LayoutSettings() {
  const [open, setOpen] = useState(false);
  const { initialState, setInitialState } = useModel('@@initialState');

  return <div className={styles.settings}>
    <Button type="text" icon={<SettingOutlined />} aria-label="界面设置"
      title="界面设置" onClick={() => setOpen(true)} />
    {open && <SettingDrawer
      collapse={open}
      onCollapseChange={setOpen}
      settings={normalizeLayoutSettings(initialState?.settings)}
      colorList={false}
      hideHintAlert
      hideCopyButton
      disableUrlParams
      onSettingChange={(value) => {
        const settings = normalizeLayoutSettings(value);
        setInitialState(current => current ? { ...current, settings } : current);
        try {
          saveLayoutSettings(settings);
        }
        catch {
          message.warning('界面已更新，但浏览器未能保存设置，刷新后将恢复默认。');
        }
      }}
    />}
  </div>;
}
