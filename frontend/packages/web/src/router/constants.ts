// 路由白名单，无需校验权限与登录状态
export const WHITE_LIST = [{ name: 'notFound', path: '/notFound', children: [] }];

// 404 路由
export const NOT_FOUND = {
  name: 'notFound',
};

// 重定向中转站路由
export const REDIRECT_ROUTE_NAME = 'Redirect';

export const WHITE_LIST_NAME = WHITE_LIST.map((el) => el.name);

// 无资源/权限路由
export const NO_RESOURCE_ROUTE_NAME = 'no-resource';
