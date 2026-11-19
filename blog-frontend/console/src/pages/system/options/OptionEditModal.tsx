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
        message.error(t('请输入有效的数字', 'Enter a valid number'));
        return;
      }

      setSaving(true);
      const updated = { ...option, name, value: converted, description, open: isSiteSetting || open, valueType };
      await updateOption(updated);
      message.success(t('保存成功', 'Saved'));
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
          <Switch defaultChecked={option.value == 'true'} checkedChildren={t('开启', 'On')} unCheckedChildren={t('关闭', 'Off')}
                  onChange={checked => form.setFieldsValue({ value: checked ? 'true' : 'false' })}/>
      );
    }
    if (option.valueType === 'number') {
      return <InputNumber style={{ width: '100%' }} placeholder={t('请输入数字', 'Enter a number')}/>;
    }
    return <Input placeholder={t('请输入配置值', 'Enter a value')}/>;
  };

  if (!option) {
    return <></>
  }

  return (
      <Modal
          title={t('编辑配置', 'Edit setting')}
          visible={visible}
          onCancel={onCancel}
          width={600}
          onOk={handleSave}
          confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          <Form.Item label={t('配置项名称', 'Setting name')}>
            <Input value={option.name} disabled/>
          </Form.Item>
          <Form.Item label={t('配置值', 'Value')} name="value" rules={[
            { required: true, message: t('请输入配置值', 'Enter a value') },
            ...(option.name === SITE_TIME_ZONE ? [{
              validator: (_, value) => !value || isTimeZone(value) ? Promise.resolve()
                : Promise.reject(new Error(t('请输入有效的 IANA 时区，例如 Asia/Shanghai', 'Enter a valid IANA time zone, e.g. Asia/Shanghai'))),
            }] : []),
          ]}>
            {renderValueInput(option)}
          </Form.Item>
          <Form.Item label={t('描述', 'Description')} name="description">
            <Input.TextArea rows={3} placeholder={t('请输入描述', 'Enter a description')} allowClear/>
          </Form.Item>
          <Form.Item label={t('公开状态', 'Visibility')} name="open" valuePropName="checked">
            <Switch disabled={isSiteSetting} checkedChildren={t('公开', 'Public')} unCheckedChildren={t('仅内部使用', 'Internal')}/>
          </Form.Item>
        </Form>
      </Modal>
  );
};

export default OptionEditModal;
