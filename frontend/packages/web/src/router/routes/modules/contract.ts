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
    permissions: ['CONTRACT:READ', 'CLUE_MANAGEMENT_POOL:READ'], // TODO lmy permission
    icon: 'iconicon_clue', // TODO lmy icon
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
        permissions: ['CLUE_MANAGEMENT_POOL:READ'], // TODO lmy permission
      },
    },
  ],
};

export default contract;
