import { OrderRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const contract: AppRouteRecordRaw = {
  path: '/order',
  name: OrderRouteEnum.ORDER,
  redirect: '/order/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'module.order',
    permissions: ['CONTRACT:READ', 'CONTRACT_PAYMENT_PLAN:READ'], // todo
    icon: 'iconicon_order_form',
    hideChildrenInMenu: true,
    collapsedLocale: 'module.order',
  },
  children: [
    {
      path: 'index',
      name: OrderRouteEnum.ORDER_INDEX,
      component: () => import('@/views/opportunity/index.vue'), // todo 替换订单页面
      meta: {
        locale: 'module.order',
        isTopMenu: true,
        permissions: [], // todo
      },
    },
  ],
};

export default contract;
