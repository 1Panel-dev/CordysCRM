import type { FormItemRule, TreeOption } from 'naive-ui';

export interface CrmTreeFieldNames {
  keyField: string;
  labelField: string;
  disabledField?: string;
  childrenField: string;
  [key: string]: any;
}

export type CrmTreeNodeData = {
  hideMoreAction?: boolean; // 隐藏更多操作
  parentId?: string;
  expanded?: boolean; // 是否展开
  [key: string]: any;
} & TreeOption;

export interface VirtualScrollPropsType {
  virtualScroll: boolean; // 是否开启虚拟滚动
  virtualScrollHeight: string; // 虚拟滚动树高度
}

export interface CrmInfoNode {
  option: CrmTreeNodeData;
  checked: boolean;
  selected: boolean;
  editing: boolean;
  [key: string]: any;
}

export interface FieldConfig {
  name: string;
  rules?: FormItemRule | Array<FormItemRule>;
  placeholder?: string;
  maxLength?: number;
  isTextArea?: boolean;
  nameExistTipText?: string; // 添加重复提示文本
}
