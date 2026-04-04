import React, { useEffect } from 'react';
import { Form, Input, InputNumber, message, Modal, Switch } from 'antd';
import { updateOption } from './service';
import { OptionItem, OptionValueType } from './types.d';


interface OptionEditModalProps {
  visible: boolean;
  option: OptionItem | null;
  onSuccess?: () => void;  // 保存成功后的回调（如刷新列表）
  onCancel: () => void;
}

interface ConvertAndValidateResult {
  valid: boolean;
  converted: string;
  error?: string;
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
        return { valid: false, converted: '', error: '请输入有效的数字' };
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

  useEffect(() => {
    if (option && visible) {
      form.setFieldsValue(option);
    }
  }, [option, visible, form]);

  const handleSave = async () => {
    if (!option) return;
    try {
      const values = await form.validateFields();
      const { value: rawValue, description, open } = values;
      const { valueType, name } = option;

      const { valid, converted, error } = convertAndValidate(rawValue, valueType);
      if (!valid) {
        message.error(error);
        return;
      }

      setSaving(true);
      await updateOption({ name, value: converted, description, open, valueType });
      message.success('保存成功');
      onSuccess && onSuccess(); // 通知父组件刷新
    }
    catch (error) {
      console.error('保存失败', error);
      message.error('保存失败，请重试');
    }
    finally {
      setSaving(false);
    }
  };

  const renderValueInput = (option: OptionItem) => {
    if (option.valueType === 'bool') {
      return (
          <Switch defaultChecked={option.value == 'true'} checkedChildren="开启" unCheckedChildren="关闭"
                  onChange={checked => form.setFieldsValue({ value: checked ? 'true' : 'false' })}/>
      );
    }
    if (option.valueType === 'number') {
      return <InputNumber style={{ width: '100%' }} placeholder="请输入数字"/>;
    }
    return <Input placeholder="请输入配置值"/>;
  };

  if (!option) {
    return <></>
  }

  return (
      <Modal
          title="编辑配置"
          visible={visible}
          onCancel={onCancel}
          width={600}
          onOk={handleSave}
          confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="配置项名称">
            <Input value={option.name} disabled/>
          </Form.Item>
          <Form.Item label="配置值" name="value" rules={[{ required: true, message: '请输入配置值' }]}>
            {renderValueInput(option)}
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} placeholder="请输入描述" allowClear/>
          </Form.Item>
          <Form.Item label="公开状态" name="open" valuePropName="checked">
            <Switch checkedChildren="公开" unCheckedChildren="仅内部使用"/>
          </Form.Item>
        </Form>
      </Modal>
  );
};

export default OptionEditModal;