import type { OperatorEnum } from '@lib/shared/enums/commonEnum';
import type { FieldDataSourceTypeEnum, FieldRuleEnum, FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import type { CollaborationType, ModuleField } from '@lib/shared/models/customer';

import type { FormItemRule } from 'naive-ui';
import type { Option } from 'naive-ui/es/transfer/src/interface';

export interface FormCreateFieldOption extends Option {
  [key: string]: any;
}

export interface FormCreateFieldRule extends FormItemRule {
  key: FieldRuleEnum;
  label?: string;
  regex?: string;
}

export interface FormCreateFieldShowControlRule {
  value?: string | number; // 当前 option 的 value
  fieldIds: string[]; // 控制的字段id集合
}

export type FormCreateFieldDateType = 'month' | 'date' | 'datetime';

export interface DataSourceFilterItem {
  leftFieldId: string | undefined; // 左侧字段id
  leftFieldType: FieldTypeEnum; // 左侧字段类型
  operator: OperatorEnum | undefined; // 操作符
  rightFieldId: string | undefined; // 右侧字段id
  rightFieldCustom?: boolean; // 右侧是否为自定义值
  rightFieldCustomValue?: string; // 右侧自定义值
  rightFieldType: FieldTypeEnum; // 右侧字段类型
}

export interface DataSourceFilterCombine {
  searchMode: 'AND' | 'OR'; // 匹配模式
  conditions: DataSourceFilterItem[]; // 条件集合
}

export interface FieldLinkOption {
  current: string[]; // 当前选项 value 集合
  method: 'AUTO' | 'HIDDEN'; // 联动方式
  target: string | string[]; // 目标选项 value 集合
}

export interface FieldLinkProp {
  targetField: string; // 目标字段id
  linkOptions: FieldLinkOption[]; // 联动选项
}

export interface FormCreateField {
  // 基础属性
  id: string;
  name: string;
  type: FieldTypeEnum;
  businessKey?: string; // 业务标准字段，不能删除
  disabledProps?: string[]; // 禁用的属性集合
  internalKey?: string;
  key?: string;
  showLabel: boolean;
  placeholder?: string;
  description: string;
  readable: boolean;
  editable: boolean;
  fieldWidth: number;
  defaultValue?: any;
  rules: FormCreateFieldRule[];
  mobile?: boolean; // 是否在移动端显示
  // 数字输入属性
  max?: number;
  min?: number;
  numberFormat?: 'number' | 'percent'; // 数字格式, number: 数字, percent: 百分比
  decimalPlaces?: boolean; // 保留小数点位
  precision?: number; // 精度
  showThousandsSeparator?: boolean; // 是否显示千分位
  // 日期输入属性
  dateType?: FormCreateFieldDateType;
  dateDefaultType?: 'custom' | 'current'; // 日期默认类型, custom: 自定义, current: 填写当时
  // radio属性
  direction?: 'horizontal' | 'vertical';
  // divider属性
  dividerClass?: string;
  dividerColor?: string;
  titleColor?: string;
  // 图片上传属性
  pictureShowType?: 'card' | 'list';
  uploadLimit?: number | null;
  uploadLimitEnable?: boolean;
  uploadSizeLimit?: number | null;
  uploadSizeLimitEnable?: boolean;
  // 地址属性
  locationType?: 'PCD' | 'PC' | 'detail' | 'C' | 'P'; // C:国家, P:国家-省,PC: 省市, PCD: 省市区, detail: 省市区+详细地址
  // 选择器属性
  multiple?: boolean;
  options?: FormCreateFieldOption[];
  initialOptions?: any[]; // 用于回显(成员、部门、数据源选择)
  // dataSource属性
  dataSourceType?: FieldDataSourceTypeEnum;
  combineSearch?: DataSourceFilterCombine; // 数据源过滤条件
  showFields?: string[]; // 数据源显示字段
  // 成员属性
  hasCurrentUser?: boolean;
  // 部门属性
  hasCurrentUserDept?: boolean;
  // 显隐控制属性(该属性是或运算，满足一个值即显示)
  showControlRules?: FormCreateFieldShowControlRule[];
  // 流水号属性
  serialNumberRules?: (string | number)[]; // 流水号规则
  // 字段联动属性
  linkProp?: FieldLinkProp;
  // 附件属性
  onlyOne?: boolean; // 是否只允许上传一个文件
  accept?: string; // 附件类型
  limitSize?: string; // 附件大小限制
  // 前端渲染属性
  icon: string;
  show?: boolean; // 是否显示，受控于别的字段的showControlRules
  linkRange?: (string | number)[]; // 联动限制可选范围
  // 链接
  linkSource?: string;
  openMode?: string;
  // 手机号
  format?: string;
  formula?: string; // 计算公式
  // 子表格
  subFields?: FormCreateField[];
  fixedColumn?: number; // 固定列数
  sumColumn?: string[]; // 需要汇总的字段id集合
}

export interface AttachmentInfo {
  id: string;
  name: string;
  organizationId: string;
  resourceId: string;
  size: number;
  storage: string;
  type: string;
  createUser: string;
  createTime: number;
}

export interface FormDetail {
  moduleFields: ModuleField[];
  optionMap?: Record<string, any[]>;
  collaborationType?: CollaborationType; // 协作类型
  attachmentMap?: Record<string, AttachmentInfo[]>; // 附件信息映射
  [key: string]: any;
}
