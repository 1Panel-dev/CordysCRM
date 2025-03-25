import { SystemRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const system: AppRouteRecordRaw = {
  path: '/system',
  name: SystemRouteEnum.SYSTEM,
  redirect: '/system/role',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: ['SYS_ORGANIZATION:READ', 'SYSTEM_ROLE:READ', 'MODULE_SETTING:READ', 'SYSTEM_SETTING:READ'],
  },
  children: [
    {
      path: 'org',
      name: SystemRouteEnum.SYSTEM_ORG,
      component: () => import('@/views/system/org/index.vue'),
      meta: {
        permissions: ['SYS_ORGANIZATION:READ'],
      },
    },
    {
      path: 'role',
      name: SystemRouteEnum.SYSTEM_ROLE,
      component: () => import('@/views/system/role/index.vue'),
      meta: {
        permissions: ['SYSTEM_ROLE:READ'],
      },
    },
    {
      path: 'module',
      name: SystemRouteEnum.SYSTEM_MODULE,
      component: () => import('@/views/system/module/index.vue'),
      meta: {
        permissions: ['MODULE_SETTING:READ'],
      },
    },
    {
      path: 'business',
      name: SystemRouteEnum.SYSTEM_BUSINESS,
      component: () => import('@/views/system/business/index.vue'),
      meta: {
        permissions: ['SYSTEM_SETTING:READ'],
      },
    },
    {
      path: 'message',
      name: SystemRouteEnum.SYSTEM_MESSAGE,
      component: () => import('@/views/system/message/index.vue'),
      meta: {
        permissions: ['SYSTEM_NOTICE:READ'],
      },
    },
    {
      path: 'log',
      name: SystemRouteEnum.SYSTEM_LOG,
      component: () => import('@/views/system/log/index.vue'),
      meta: {
        permissions: [],
      },
    },
  ],
};

export default system;
