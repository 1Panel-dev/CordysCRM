import { DashboardRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const dashboard: AppRouteRecordRaw = {
  path: '/dashboard',
  name: DashboardRouteEnum.DASHBOARD,
  redirect: '/dashboard/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.dashboard',
    permissions: [],
    icon: 'iconicon_dashboard1',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.dashboard',
  },
  children: [
    {
      path: 'index',
      name: DashboardRouteEnum.DASHBOARD_INDEX,
      component: () => import('@/views/dashboard/index.vue'),
      meta: {
        locale: 'menu.dashboard',
        permissions: [],
      },
    },
  ],
};

export default dashboard;
