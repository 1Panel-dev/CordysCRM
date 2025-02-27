import { OpportunityRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const system: AppRouteRecordRaw = {
  path: '/opportunity',
  name: OpportunityRouteEnum.OPPORTUNITY,
  redirect: '/opportunity/opt',
  component: DEFAULT_LAYOUT,
  children: [
    {
      path: 'opt',
      name: OpportunityRouteEnum.OPPORTUNITY_OPT,
      component: () => import('@/views/opportunity/index.vue'),
    },
  ],
};

export default system;
