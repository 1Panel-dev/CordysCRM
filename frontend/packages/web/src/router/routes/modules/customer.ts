import { CustomerRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const customer: AppRouteRecordRaw = {
  path: '/customer',
  name: CustomerRouteEnum.CUSTOMER,
  redirect: '/customer/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'module.customerManagement',
    permissions: ['CUSTOMER_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT_POOL:READ', 'CUSTOMER_MANAGEMENT_CONTACT:READ'],
    icon: 'iconicon_multiple_choice_of_members',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.customer',
  },
  children: [
    {
      path: 'index',
      name: CustomerRouteEnum.CUSTOMER_INDEX,
      component: () => import('@/views/customer/customer.vue'),
      meta: {
        locale: 'menu.customer',
        isTopMenu: true,
        permissions: ['CUSTOMER_MANAGEMENT:READ'],
      },
    },
    {
      path: 'contact',
      name: CustomerRouteEnum.CUSTOMER_CONTACT,
      component: () => import('@/views/customer/contact.vue'),
      meta: {
        locale: 'menu.contact',
        isTopMenu: true,
        permissions: ['CUSTOMER_MANAGEMENT_CONTACT:READ'],
      },
    },
    {
      path: 'openSea',
      name: CustomerRouteEnum.CUSTOMER_OPEN_SEA,
      component: () => import('@/views/customer/openSea.vue'),
      meta: {
        locale: 'module.openSea',
        isTopMenu: true,
        permissions: ['CUSTOMER_MANAGEMENT_POOL:READ'],
      },
    },
  ],
};

export default customer;
