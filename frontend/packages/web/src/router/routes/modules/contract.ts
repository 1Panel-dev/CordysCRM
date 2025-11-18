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
    permissions: ['CLUE_MANAGEMENT:READ', 'CLUE_MANAGEMENT_POOL:READ'], // TODO lmy
    icon: 'iconicon_clue', // TODO lmy
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
        permissions: ['CLUE_MANAGEMENT:READ'], // TODO lmy
      },
    },
    {
      path: 'contractVoided',
      name: ContractRouteEnum.CONTRACT_VOIDED,
      component: () => import('@/views/contract/contractVoided/index.vue'),
      meta: {
        locale: 'module.voidedAgreement',
        isTopMenu: true,
        permissions: ['CLUE_MANAGEMENT_POOL:READ'], // TODO lmy
      },
    },
    {
      path: 'contractPaymentPlan',
      name: ContractRouteEnum.CONTRACT_PAYMENT,
      component: () => import('@/views/contract/contractPaymentPlan/index.vue'),
      meta: {
        locale: 'module.paymentPlan',
        isTopMenu: true,
        permissions: ['CLUE_MANAGEMENT_POOL:READ'], // TODO lmy
      },
    },
  ],
};

export default contract;
