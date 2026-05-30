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
    collapsedLocale: 'menu.collapsedProduct',
  },
  children: [
    {
      path: 'pro',
      name: ProductRouteEnum.PRODUCT_PRO,
      component: () => import('@/views/product/index.vue'),
      meta: {
        locale: 'module.productManagement',
        permissions: ['PRODUCT_MANAGEMENT:READ'],
        isTopMenu: true,
      },
    },
    {
      path: 'price',
      name: ProductRouteEnum.PRODUCT_PRICE,
      component: () => import('@/views/product/price.vue'),
      meta: {
        locale: 'module.productManagementPrice',
        permissions: ['PRICE:READ'],
        isTopMenu: true,
      },
    },
  ],
};

export default product;
