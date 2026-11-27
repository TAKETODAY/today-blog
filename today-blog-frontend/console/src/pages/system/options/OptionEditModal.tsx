import React, { useEffect } from 'react';
import { Form, Input, InputNumber, message, Modal, Select, Switch } from 'antd';
import { useSiteTranslation } from '@/utils/use-site-translation';
import { isTimeZone, localeOptions, SITE_LOCALE, SITE_TIME_ZONE } from '../../../../../common/site-settings';
import { updateOption } from './service';
import { OptionItem, OptionValueType } from './types';


interface OptionEditModalProps {
  visible: boolean;
  option: OptionItem | null;
  onSuccess?: (option: OptionItem) => void;
  onCancel: () => void;
}

interface ConvertAndValidateResult {
  valid: boolean;
  converted: string;
}

// 类型校验与转换
const convertAndValidate = (rawValue: any, valueType: OptionValueType): ConvertAndValidateResult => {
  switch (valueType) {
    case 'bool':
      const boolValue = typeof rawValue === 'boolean' ? rawValue : rawValue === 'true';
      return { valid: true, converted: boolValue ? 'true' : 'false' };
    case 'number':
      const num = Number(rawValue);
      if (isNaN(num)) {
        return { valid: false, converted: '' };
      }
      return { valid: true, converted: String(num) };
    case 'string':
    default:
      return { valid: true, converted: String(rawValue) };
  }
};

const OptionEditModal: React.FC<OptionEditModalProps> = ({ visible, option, onSuccess, onCancel }) => {
  const [form] = Form.useForm();
  const [saving, setSaving] = React.useState(false);
  const t = useSiteTranslation();
  const isSiteSetting = option?.name === SITE_LOCALE || option?.name === SITE_TIME_ZONE;

  useEffect(() => {
    if (option && visible) {
      form.setFieldsValue(option);
    }
  }, [option, visible, form]);

  const handleSave = async () => {
    if (!option) {
      return;
    }
    const values = await form.validateFields().catch(() => null);
    if (!values) {
      return;
    }
    try {
      const { value: rawValue, description, open } = values;
      const { valueType, name } = option;

      const { valid, converted } = convertAndValidate(rawValue, valueType);
      if (!valid) {
        message.error(t('options.enterAValidNumber'));
        return;
      }

      setSaving(true);
      const updated = { ...option, name, value: converted, description, open: isSiteSetting || open, valueType };
      await updateOption(updated);
      message.success(t('options.saved'));
      onSuccess?.(updated);
    }
    catch (error) {
      console.error('Could not save option', error);
    }
    finally {
      setSaving(false);
    }
  };

  const renderValueInput = (option: OptionItem) => {
    if (option.name === SITE_LOCALE) {
      return <Select options={localeOptions}/>;
    }
    if (option.name === SITE_TIME_ZONE) {
      return <Input placeholder="Asia/Shanghai"/>;
    }
    if (option.valueType === 'bool') {
      return (
          <Switch defaultChecked={option.value == 'true'} checkedChildren={t('options.on')} unCheckedChildren={t('options.off')}
                  onChange={checked => form.setFieldsValue({ value: checked ? 'true' : 'false' })}/>
      );
    }
    if (option.valueType === 'number') {
      return <InputNumber style={{ width: '100%' }} placeholder={t('options.enterANumber')}/>;
    }
    return <Input placeholder={t('options.enterAValue')}/>;
  };

  if (!option) {
    return <></>
  }

  return (
      <Modal
          title={t('options.editSetting')}
          visible={visible}
          onCancel={onCancel}
          width={600}
          onOk={handleSave}
          confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          <Form.Item label={t('options.settingName')}>
            <Input value={option.name} disabled/>
          </Form.Item>
          <Form.Item label={t('options.value')} name="value" rules={[
            { required: true, message: t('options.enterAValue') },
            ...(option.name === SITE_TIME_ZONE ? [{
              validator: (_, value) => !value || isTimeZone(value) ? Promise.resolve()
                : Promise.reject(new Error(t('options.enterAValidIANATime'))),
            }] : []),
          ]}>
            {renderValueInput(option)}
          </Form.Item>
          <Form.Item label={t('options.description')} name="description">
            <Input.TextArea rows={3} placeholder={t('options.enterADescription')} allowClear/>
          </Form.Item>
          <Form.Item label={t('options.visibility')} name="open" valuePropName="checked">
            <Switch disabled={isSiteSetting} checkedChildren={t('options.public')} unCheckedChildren={t('options.internal')}/>
          </Form.Item>
        </Form>
      </Modal>
  );
};

export default OptionEditModal;
