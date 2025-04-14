import { WorkbenchRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const workbench: AppRouteRecordRaw = {
  path: '/workbench',
  name: WorkbenchRouteEnum.WORKBENCH,
  redirect: '/workbench/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'index',
      name: WorkbenchRouteEnum.WORKBENCH_INDEX,
      component: () => import('@/views/workbench/index.vue'),
      meta: {
        locale: 'menu.workbench',
        permissions: [],
        depth: 1,
      },
    },
    {
      path: 'duplicateCheck',
      name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK,
      component: () => import('@/views/workbench/duplicateCheck.vue'),
      meta: {
        locale: 'menu.duplicateCheck',
        permissions: [],
        depth: 2,
      },
    },
  ],
};

export default workbench;
