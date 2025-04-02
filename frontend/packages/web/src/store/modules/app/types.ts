import { UploadFileInfo } from 'naive-ui';

import type { ModuleNavBaseInfoItem } from '@lib/shared/models/system/module';
import type { MessageInfo } from '@lib/shared/models/user';

import type { GlobalThemeOverrides } from 'naive-ui';
import type { RouteRecordRaw } from 'vue-router';

// 平台风格
export type Style = 'default' | 'custom' | 'follow';

// 主题
export type Theme = 'default' | 'custom';

// 主题配置对象
export interface ThemeConfig {
  style: Style;
  customStyle: string;
  theme: Theme;
  customTheme: string;
}

// 登录页配置对象
export interface LoginConfig {
  title: string;
  icon: (UploadFileInfo | never)[];
  loginLogo: (UploadFileInfo | never)[];
  loginImage: (UploadFileInfo | never)[];
  slogan: string;
}

//  平台配置对象
export interface PlatformConfig {
  logoPlatform: (UploadFileInfo | never)[];
  platformName: string;
  helpDoc: string;
}

//  界面配置对象
export interface PageConfig extends ThemeConfig, LoginConfig, PlatformConfig {}

export interface AppState {
  pageSize: number;
  showSizePicker: boolean;
  showQuickJumper: boolean;
  menuCollapsed: boolean; // 侧边菜单栏是否收缩
  collapsedWidth: number; // 侧边菜单栏收缩宽度
  loginLoading: boolean; // 登录页面加载中
  loading: boolean; // 全局加载中
  loadingTip: string; // 全局加载提示
  pageConfig: PageConfig;
  defaultThemeConfig: ThemeConfig;
  defaultLoginConfig: LoginConfig;
  defaultPlatformConfig: PlatformConfig;
  themeOverridesConfig: GlobalThemeOverrides;
  orgId: string;
  moduleConfigList: ModuleNavBaseInfoItem[]; // 模块配置列表
  topMenus: RouteRecordRaw[];
  currentTopMenu: RouteRecordRaw;
  messageInfo: MessageInfo; // 消息通知和公告
  eventSource: null | EventSource; // 事件流资源
  menuIconStatus: boolean;
}
