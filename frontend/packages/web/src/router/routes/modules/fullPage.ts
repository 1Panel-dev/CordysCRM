import { FullPageEnum } from '@/enums/routeEnum';

import { FULL_PAGE_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const FullPage: AppRouteRecordRaw = {
  path: '/fullPage',
  name: FullPageEnum.FULL_PAGE,
  redirect: '/fullPage/fullPageDashboard',
  component: FULL_PAGE_LAYOUT,
  meta: {
    hideInMenu: true,
    permissions: ['DASHBOARD:READ'],
  },
  children: [
    {
      path: 'fullPageDashboard',
      name: FullPageEnum.FULL_PAGE_DASHBOARD,
      component: () => import('@/views/dashboard/fullPage.vue'),
      meta: {
        permissions: ['DASHBOARD:READ'],
      },
    },
  ],
};

export default FullPage;
