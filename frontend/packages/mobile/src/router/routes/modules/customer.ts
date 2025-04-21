import { CustomerRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const customer: AppRouteRecordRaw = {
  path: '/customer',
  name: CustomerRouteEnum.CUSTOMER,
  redirect: '/customer/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: ['CUSTOMER_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT_POOL:READ', 'CUSTOMER_MANAGEMENT_CONTACT:READ'],
  },
  children: [
    {
      path: 'index',
      name: CustomerRouteEnum.CUSTOMER_INDEX,
      component: () => import('@/views/customer/index.vue'),
      meta: {
        locale: 'menu.customer',
        depth: 1,
        isCache: true,
        permissions: ['CUSTOMER_MANAGEMENT:READ'],
      },
    },
    {
      path: 'detail',
      name: CustomerRouteEnum.CUSTOMER_DETAIL,
      component: () => import('@/views/customer/detail.vue'),
      meta: {
        depth: 2,
        isCache: true,
        permissions: ['CUSTOMER_MANAGEMENT:READ'],
      },
    },
    {
      path: 'transfer',
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      component: () => import('@/views/customer/transfer.vue'),
      meta: {
        depth: 3,
        permissions: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
    },
    {
      path: 'distribute',
      name: CustomerRouteEnum.CUSTOMER_DISTRIBUTE,
      component: () => import('@/views/customer/openSea/distribute.vue'),
      meta: {
        depth: 3,
        permissions: ['CUSTOMER_MANAGEMENT_POOL:ASSIGN'],
      },
    },
    {
      path: 'openSeaDetail',
      name: CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL,
      component: () => import('@/views/customer/openSea/detail.vue'),
      meta: {
        depth: 2,
        permissions: ['CUSTOMER_MANAGEMENT_POOL:READ'],
      },
    },
    {
      path: 'relation',
      name: CustomerRouteEnum.CUSTOMER_RELATION,
      component: () => import('@/views/customer/relation.vue'),
      meta: {
        depth: 3,
        permissions: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
    },
    {
      path: 'collaborator',
      name: CustomerRouteEnum.CUSTOMER_COLLABORATOR,
      component: () => import('@/views/customer/collaborator.vue'),
      meta: {
        depth: 3,
        permissions: ['CUSTOMER_MANAGEMENT:UPDATE'],
      },
    },
  ],
};

export default customer;
