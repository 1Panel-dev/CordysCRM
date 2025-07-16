import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';

import {
  ClueRouteEnum,
  CustomerRouteEnum,
  DashboardRouteEnum,
  OpportunityRouteEnum,
  ProductRouteEnum,
  SystemRouteEnum,
  WorkbenchRouteEnum,
} from '@/enums/routeEnum';

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
export const NO_RESOURCE_ROUTE_NAME_INDEX = 'no-resource-index';

// 首页路由
export const DEFAULT_ROUTE_NAME = 'workbench';

// 模块（有开关）路由
export const featureRouteMap: Record<string, any> = {
  [WorkbenchRouteEnum.WORKBENCH]: ModuleConfigEnum.HOME,
  [CustomerRouteEnum.CUSTOMER]: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
  [OpportunityRouteEnum.OPPORTUNITY]: ModuleConfigEnum.BUSINESS_MANAGEMENT,
  [ProductRouteEnum.PRODUCT]: ModuleConfigEnum.PRODUCT_MANAGEMENT,
  [ClueRouteEnum.CLUE_MANAGEMENT]: ModuleConfigEnum.CLUE_MANAGEMENT,
  [DashboardRouteEnum.DASHBOARD]: ModuleConfigEnum.DASHBOARD,
};

export const allMenuRouteMap: Record<string, any> = {
  ...featureRouteMap,
  [SystemRouteEnum.SYSTEM]: ModuleConfigEnum.SYSTEM_SETTINGS,
};
