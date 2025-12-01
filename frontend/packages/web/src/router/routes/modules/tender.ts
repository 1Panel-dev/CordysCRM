import { TenderRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const tender: AppRouteRecordRaw = {
  path: '/tender',
  name: TenderRouteEnum.TENDER,
  redirect: '/tender/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.tender',
    permissions: ['TENDER:READ'],
    icon: 'iconicon_dashboard1',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.tender',
  },
  children: [
    {
      path: 'index',
      name: TenderRouteEnum.TENDER_INDEX,
      component: () => import('@/views/tender/index.vue'),
      meta: {
        locale: 'menu.tender',
        permissions: ['TENDER:READ'],
      },
    },
  ],
};

export default tender;
