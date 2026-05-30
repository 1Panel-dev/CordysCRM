// 404 路由
export const NOT_FOUND = {
  name: 'notFound',
  path: '/not-found',
  meta: {
    requiresAuth: false,
  },
};

// 路由白名单，无需校验权限与登录状态
export const WHITE_LIST = [NOT_FOUND];

// 重定向中转站路由
export const REDIRECT_ROUTE_NAME = 'Redirect';

export const WHITE_LIST_NAME = WHITE_LIST.map((el) => el.name);

// 无资源/权限路由
export const NO_RESOURCE_ROUTE_NAME = 'no-resource';

// 企业微信认证被禁用路由
export const AUTH_DISABLED_ROUTE_NAME = 'auth-disabled';

// 首页路由
export const DEFAULT_ROUTE_NAME = 'workbench';

// 登录加载路由
export const LOGIN_LOADING = 'loading';
