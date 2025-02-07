import { UploadFileInfo } from 'naive-ui';

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
