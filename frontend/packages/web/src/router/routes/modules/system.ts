import { SystemRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const system: AppRouteRecordRaw = {
  path: '/system',
  name: SystemRouteEnum.SYSTEM,
  redirect: '/system/role',
  component: DEFAULT_LAYOUT,
  children: [
    {
      path: 'org',
      name: SystemRouteEnum.SYSTEM_ORG,
      component: () => import('@/views/system/org/index.vue'),
    },
    {
      path: 'role',
      name: SystemRouteEnum.SYSTEM_ROLE,
      component: () => import('@/views/system/role/index.vue'),
    },
    {
      path: 'module',
      name: SystemRouteEnum.SYSTEM_MODULE,
      component: () => import('@/views/system/module/index.vue'),
    },
    {
      path: 'business',
      name: SystemRouteEnum.SYSTEM_BUSINESS,
      component: () => import('@/views/system/business/index.vue'),
    },
    {
      path: 'log',
      name: SystemRouteEnum.SYSTEM_LOG,
      component: () => import('@/views/system/log/index.vue'),
    },
  ],
};

export default system;
