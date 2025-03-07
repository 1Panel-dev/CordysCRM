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
      component: () => import('@/views/customer/customer.vue'),
      meta: {
        locale: 'menu.customer',
        isTopMenu: true,
        permissions: [],
      },
    },
    {
      path: 'contact',
      name: CustomerRouteEnum.CUSTOMER_CONTACT,
      component: () => import('@/views/customer/contact.vue'),
      meta: {
        locale: 'menu.contact',
        isTopMenu: true,
        permissions: [],
      },
    },
    {
      path: 'openSea',
      name: CustomerRouteEnum.CUSTOMER_OPEN_SEA,
      component: () => import('@/views/customer/openSea.vue'),
      meta: {
        locale: 'module.openSea',
        isTopMenu: true,
        permissions: [],
      },
    },
  ],
};

export default customer;
