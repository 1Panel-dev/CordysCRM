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
    permissions: ['DASHBOARD:READ'],
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
        permissions: ['DASHBOARD:READ'],
      },
    },
    {
      path: 'link',
      name: DashboardRouteEnum.DASHBOARD_LINK,
      component: () => import('@/views/dashboard/link.vue'),
      meta: {
        permissions: ['DASHBOARD:READ'],
        hideInMenu: true,
        breadcrumbs: [
          {
            name: DashboardRouteEnum.DASHBOARD,
            locale: 'menu.dashboard',
          },
          {
            name: DashboardRouteEnum.DASHBOARD_LINK,
            locale: 'system.business.DE.link',
          },
        ],
      },
    },
    {
      path: 'module',
      name: DashboardRouteEnum.DASHBOARD_MODULE,
      component: () => import('@/views/dashboard/module.vue'),
      meta: {
        permissions: ['DASHBOARD:READ'],
        hideInMenu: true,
        breadcrumbs: [
          {
            name: DashboardRouteEnum.DASHBOARD,
            locale: 'menu.dashboard',
          },
          {
            name: DashboardRouteEnum.DASHBOARD_MODULE,
            locale: 'system.business.DE.embedModule',
          },
        ],
      },
    },
  ],
};

export default dashboard;
