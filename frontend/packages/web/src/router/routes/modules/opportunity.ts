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
    permissions: ['OPPORTUNITY_MANAGEMENT:READ'],
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
        permissions: ['OPPORTUNITY_MANAGEMENT:READ'],
      },
    },
  ],
};

export default system;
