import { CustomerRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const customer: AppRouteRecordRaw = {
  path: '/customer',
  name: CustomerRouteEnum.CUSTOMER,
  redirect: '/customer/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'index',
      name: CustomerRouteEnum.CUSTOMER_INDEX,
      component: () => import('@/views/customer/index.vue'),
      meta: {
        locale: 'menu.customer',
        permissions: [],
      },
    },
  ],
};

export default customer;
