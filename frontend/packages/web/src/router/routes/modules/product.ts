import { ProductRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const product: AppRouteRecordRaw = {
  path: '/product',
  name: ProductRouteEnum.PRODUCT,
  redirect: '/product/pro',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: ['PRODUCT_MANAGEMENT:READ'],
  },
  children: [
    {
      path: 'pro',
      name: ProductRouteEnum.PRODUCT_PRO,
      component: () => import('@/views/product/index.vue'),
      meta: {
        permissions: ['PRODUCT_MANAGEMENT:READ'],
      },
    },
  ],
};

export default product;
