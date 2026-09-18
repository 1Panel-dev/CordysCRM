import { TabPaneProps } from 'naive-ui';

export type TabContentItem = {
  enable: boolean;
  permission?: string[]; // 权限标识
  // 系统详情标签的稳定标识，用于匹配表单配置中的 detailTabs。
  internalKey?: string;
} & TabPaneProps;

export interface ContentTabsMap {
  tabList: TabContentItem[];
}
