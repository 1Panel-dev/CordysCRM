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
    {
      path: 'detail',
      name: CustomerRouteEnum.CUSTOMER_DETAIL,
      component: () => import('@/views/customer/detail.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'transfer',
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      component: () => import('@/views/customer/transfer.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'distribute',
      name: CustomerRouteEnum.CUSTOMER_DISTRIBUTE,
      component: () => import('@/views/customer/openSea/distribute.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'openSeaDetail',
      name: CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL,
      component: () => import('@/views/customer/openSea/detail.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'relation',
      name: CustomerRouteEnum.CUSTOMER_RELATION,
      component: () => import('@/views/customer/relation.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'collaborator',
      name: CustomerRouteEnum.CUSTOMER_COLLABORATOR,
      component: () => import('@/views/customer/collaborator.vue'),
      meta: {
        permissions: [],
      },
    },
  ],
};

export default customer;
