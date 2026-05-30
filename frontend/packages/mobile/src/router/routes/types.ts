import { defineComponent } from 'vue';

import type { NavigationGuard } from 'vue-router';

export type Component<T = any> =
  | ReturnType<typeof defineComponent>
  | (() => Promise<typeof import('*.vue')>)
  | (() => Promise<T>);

export interface RouteMeta {
  permissions?: string[]; // 权限数组
  requiresAuth?: boolean; // 是否需要权限，默认需要
  icon?: string; // 菜单icon
  locale?: string; // 国际化语言单词
  order?: number; // 排序权重
  depth?: number; // 路由深度
  isCache?: boolean; // 缓存设置，true缓存页面状态
}
export interface AppRouteRecordRaw {
  path: string;
  name?: string | symbol;
  meta?: RouteMeta;
  redirect?: string;
  component: Component | string;
  children?: AppRouteRecordRaw[];
  alias?: string | string[];
  props?: Record<string, any> | boolean;
  beforeEnter?: NavigationGuard | NavigationGuard[];
  fullPath?: string;
}
