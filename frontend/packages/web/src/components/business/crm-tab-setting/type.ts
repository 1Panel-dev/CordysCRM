import { TabPaneProps } from 'naive-ui';

export type TabContentItem = {
  enable: boolean;
  permission?: string[]; // 权限标识
} & TabPaneProps;

export interface ContentTabsMap {
  tabList: TabContentItem[];
  backupTabList: TabContentItem[];
}
