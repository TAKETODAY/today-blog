import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ClockCircleOutlined, TranslationOutlined } from '@ant-design/icons';
import { Dropdown, Modal, Select } from 'antd';
import 'antd/es/select/style';
import { localeOptions } from '../../../common/site-settings';
import { getCurrentTimeZone, getVisitorLocale, getVisitorTimeZone, setVisitorPreference } from '../../../common/visitor-preferences';
import { translate } from '../locales';
import './DisplaySettings.css';

export default function DisplaySettings() {
  const [open, setOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const trigger = useRef(null);
  const popup = useRef(null);
  const languageLabel = translate('navigation.displayLanguage');
  const timeZoneLabel = translate('navigation.displayTimeZone');
  const language = getVisitorLocale() || 'browser';
  const timeZone = getCurrentTimeZone();
  const timeZoneOptions = useMemo(() => [...new Set([
    'UTC', 'Asia/Shanghai', 'America/New_York', 'Europe/London', timeZone,
    ...(Intl.supportedValuesOf?.('timeZone') || []),
  ])].map(value => ({ value, label: value })), [timeZone]);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }
    const frame = requestAnimationFrame(() => {
      popup.current?.querySelector('[aria-checked="true"]')?.focus();
    });
    return () => cancelAnimationFrame(frame);
  }, [menuOpen]);

  function onMenuKeyDown(event) {
    if (event.key === 'Escape' || event.key === 'Tab') {
      event.stopPropagation();
      setMenuOpen(false);
      trigger.current?.focus();
    }
  }

  const languageItem = (key, label) => ({ key, label, role: 'menuitemradio', 'aria-checked': key === language });
  const menu = {
    selectable: true,
    selectedKeys: [language],
    items: [
      ...localeOptions.map(({ value, label }) => languageItem(value, label)),
      { type: 'divider' },
      languageItem('browser', translate('navigation.browserLanguage')),
      languageItem('site', translate('navigation.siteLanguage')),
      { type: 'divider' },
      { key: 'timezone', icon: <ClockCircleOutlined aria-hidden="true"/>, label: timeZoneLabel },
    ],
    onClick: ({ key }) => {
      setMenuOpen(false);
      if (key === 'timezone') {
        setOpen(true);
      }
      else {
        setVisitorPreference('locale', key === 'browser' ? null : key);
        trigger.current?.focus();
      }
    },
  };

  return (
    <>
      <Dropdown
        dropdownRender={content => <div ref={popup} onKeyDown={onMenuKeyDown}>{content}</div>}
        menu={menu}
        open={menuOpen}
        onOpenChange={setMenuOpen}
        placement="bottomRight"
        trigger={['click']}
      >
        <button
          ref={trigger}
          type="button"
          className="display-settings-trigger"
          aria-label={languageLabel}
          aria-haspopup="menu"
          aria-expanded={menuOpen}
          title={languageLabel}
        >
          <TranslationOutlined aria-hidden="true"/>
        </button>
      </Dropdown>
      <Modal
        open={open}
        destroyOnClose
        centered
        width={400}
        wrapClassName="display-settings-dialog"
        title={timeZoneLabel}
        footer={null}
        focusTriggerAfterClose={false}
        onCancel={() => setOpen(false)}
        afterClose={() => trigger.current?.focus()}
      >
        <p className="display-settings-hint">{translate('navigation.displayTimeZoneHint')}</p>
        <div className="display-settings-field">
          <Select
            aria-label={timeZoneLabel}
            showSearch
            optionFilterProp="label"
            value={getVisitorTimeZone() || 'site'}
            onChange={value => setVisitorPreference('timeZone', value === 'site' ? null : value)}
            options={[
              { value: 'site', label: translate('navigation.siteTimeZone') },
              ...timeZoneOptions,
            ]}
          />
        </div>
      </Modal>
    </>
  );
}
