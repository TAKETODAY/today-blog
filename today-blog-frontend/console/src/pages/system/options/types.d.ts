export type OptionValueType = 'bool' | 'string' | 'number';

// 配置项数据结构
export interface OptionItem {
  name: string;
  value: string;
  valueType: OptionValueType;
  description: string | null;
  open: boolean;
  updateAt?: Date;
}