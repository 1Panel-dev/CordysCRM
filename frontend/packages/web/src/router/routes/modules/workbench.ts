import { WorkbenchRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const workbench: AppRouteRecordRaw = {
  path: '/workbench',
  name: WorkbenchRouteEnum.WORKBENCH,
  redirect: '/workbench/index',
  component: DEFAULT_LAYOUT,
  meta: {
    hideChildrenInMenu: true,
    locale: 'menu.workbench',
    permissions: [],
    icon: 'iconicon_home',
    collapsedLocale: 'menu.workbench',
  },
  children: [
    {
      path: 'index',
      name: WorkbenchRouteEnum.WORKBENCH_INDEX,
      component: () => import('@/views/workbench/index.vue'),
      meta: {
        locale: 'menu.workbench',
        permissions: [],
      },
    },
  ],
};

export default workbench;
