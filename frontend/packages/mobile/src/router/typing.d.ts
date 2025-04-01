import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    permissions?: string[]; // 权限数组
    requiresAuth?: boolean; // 是否需要权限，默认需要
    icon?: string; // 菜单icon
    locale?: string; // 国际化语言单词
    collapsedLocale?: string; // 收起时的国际化语言单词
    hideInMenu?: boolean; // 此路由不在菜单展示
    hideChildrenInMenu?: boolean; // 子路由不展示在菜单
    activeMenu?: string; // 激活状态
    order?: number; // 排序权重
  }
}
