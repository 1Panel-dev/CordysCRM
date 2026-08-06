import useLicenseStore from '@/store/modules/setting/license';

import { WorkbenchRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const workbench: AppRouteRecordRaw = {
  path: '/workbench',
  name: WorkbenchRouteEnum.WORKBENCH,
  redirect: () => {
    const licenseStore = useLicenseStore();
    return {
      name: licenseStore.hasLicense() ? WorkbenchRouteEnum.WORKBENCH_SMART : WorkbenchRouteEnum.WORKBENCH_BOARD,
    };
  },
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
      path: 'smart',
      name: WorkbenchRouteEnum.WORKBENCH_SMART,
      component: () => import('@/views/workbench/smart/index.vue'),
      meta: {
        locale: 'menu.workbench.smart',
        permissions: [],
        isTopMenu: true,
      },
    },
    {
      path: 'index',
      name: WorkbenchRouteEnum.WORKBENCH_BOARD,
      component: () => import('@/views/workbench/index.vue'),
      meta: {
        locale: 'menu.workbench.board',
        permissions: [],
        isTopMenu: true,
      },
    },
  ],
};

export default workbench;
