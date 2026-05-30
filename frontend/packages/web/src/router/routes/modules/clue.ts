import { ClueRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const lead: AppRouteRecordRaw = {
  path: '/lead',
  name: ClueRouteEnum.CLUE_MANAGEMENT,
  redirect: '/lead/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'module.clueManagement',
    permissions: ['CLUE_MANAGEMENT:READ', 'CLUE_MANAGEMENT_POOL:READ'],
    icon: 'iconicon_clue',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.clue',
  },
  children: [
    {
      path: 'index',
      name: ClueRouteEnum.CLUE_MANAGEMENT_CLUE,
      component: () => import('@/views/clueManagement/clue/index.vue'),
      meta: {
        locale: 'menu.clue',
        isTopMenu: true,
        permissions: ['CLUE_MANAGEMENT:READ'],
      },
    },
    {
      path: 'leadPool',
      name: ClueRouteEnum.CLUE_MANAGEMENT_POOL,
      component: () => import('@/views/clueManagement/cluePool/index.vue'),
      meta: {
        locale: 'module.cluePool',
        isTopMenu: true,
        permissions: ['CLUE_MANAGEMENT_POOL:READ'],
      },
    },
  ],
};

export default lead;
