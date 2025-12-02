import { ContractRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const contract: AppRouteRecordRaw = {
  path: '/contract',
  name: ContractRouteEnum.CONTRACT,
  redirect: '/contract/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'module.contract',
    permissions: ['CONTRACT:READ', 'CONTRACT_PAYMENT_PLAN:READ'],
    icon: 'iconicon_contract',
    hideChildrenInMenu: true,
    collapsedLocale: 'module.contract',
  },
  children: [
    {
      path: 'index',
      name: ContractRouteEnum.CONTRACT_INDEX,
      component: () => import('@/views/contract/contract/index.vue'),
      meta: {
        locale: 'module.contract',
        isTopMenu: true,
        permissions: ['CONTRACT:READ'],
      },
    },
    {
      path: 'contractPaymentPlan',
      name: ContractRouteEnum.CONTRACT_PAYMENT,
      component: () => import('@/views/contract/contractPaymentPlan/index.vue'),
      meta: {
        locale: 'module.paymentPlan',
        isTopMenu: true,
        permissions: ['CONTRACT_PAYMENT_PLAN:READ'],
      },
    },
  ],
};

export default contract;
