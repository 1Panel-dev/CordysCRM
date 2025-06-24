import { TabPaneProps } from 'naive-ui';

export type TabContentItem = {
  enable: boolean;
  allowClose?: boolean; // 允许关闭
  permission?: string[]; // 权限标识
} & TabPaneProps;

export interface ContentTabsMap {
  tabList: TabContentItem[];
  backupTabList: TabContentItem[];
}
