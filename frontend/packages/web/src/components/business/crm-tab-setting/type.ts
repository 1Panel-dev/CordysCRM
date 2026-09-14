import { TabPaneProps } from 'naive-ui';

export type TabContentItem = {
  enable: boolean;
  permission?: string[]; // 权限标识
} & TabPaneProps;

export interface ContentTabsMap {
  tabList: TabContentItem[];
  backupTabList: TabContentItem[];
  tabConfigVersion?: string; // 表单配置中当前可用标签的标识，用于判断是否需要重置个人展示配置。
}
