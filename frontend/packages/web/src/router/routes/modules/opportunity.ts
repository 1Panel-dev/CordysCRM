import { OpportunityRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const system: AppRouteRecordRaw = {
  path: '/opportunity',
  name: OpportunityRouteEnum.OPPORTUNITY,
  redirect: '/opportunity/opt',
  component: DEFAULT_LAYOUT,
  meta: {
    hideChildrenInMenu: true,
    locale: 'menu.opportunity',
    permissions: ['OPPORTUNITY_MANAGEMENT:READ', 'OPPORTUNITY_QUOTATION:READ'],
    icon: 'iconicon_business_opportunity',
    collapsedLocale: 'menu.collapsedOpportunity',
  },
  children: [
    {
      path: 'opt',
      name: OpportunityRouteEnum.OPPORTUNITY_OPT,
      component: () => import('@/views/opportunity/index.vue'),
      meta: {
        locale: 'menu.opportunity',
        isTopMenu: true,
        permissions: ['OPPORTUNITY_MANAGEMENT:READ'],
      },
    },
    {
      path: 'quotation',
      name: OpportunityRouteEnum.OPPORTUNITY_QUOTATION,
      component: () => import('@/views/opportunity/quotation.vue'),
      meta: {
        locale: 'menu.quotation',
        isTopMenu: true,
        permissions: ['OPPORTUNITY_QUOTATION:READ'],
      },
    },
  ],
};

export default system;
