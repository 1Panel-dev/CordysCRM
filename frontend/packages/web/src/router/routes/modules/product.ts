import { ProductRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const product: AppRouteRecordRaw = {
  path: '/product',
  name: ProductRouteEnum.PRODUCT,
  redirect: '/product/pro',
  component: DEFAULT_LAYOUT,
  meta: {
    hideChildrenInMenu: true,
    locale: 'module.productManagement',
    permissions: ['PRODUCT_MANAGEMENT:READ'],
    icon: 'iconicon_product',
  },
  children: [
    {
      path: 'pro',
      name: ProductRouteEnum.PRODUCT_PRO,
      component: () => import('@/views/product/index.vue'),
      meta: {
        locale: 'module.productManagement',
        permissions: ['PRODUCT_MANAGEMENT:READ'],
      },
    },
  ],
};

export default product;
