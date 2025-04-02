import { OpportunityRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const opportunity: AppRouteRecordRaw = {
  path: '/opportunity',
  name: OpportunityRouteEnum.OPPORTUNITY,
  redirect: '/opportunity/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'index',
      name: OpportunityRouteEnum.OPPORTUNITY_INDEX,
      component: () => import('@/views/opportunity/index.vue'),
      meta: {
        locale: 'menu.opportunity',
        permissions: [],
      },
    },
  ],
};

export default opportunity;
