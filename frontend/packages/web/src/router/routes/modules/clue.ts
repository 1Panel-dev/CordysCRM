import { ClueRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const clue: AppRouteRecordRaw = {
  path: '/clueManagement',
  name: ClueRouteEnum.CLUE_MANAGEMENT,
  redirect: '/clueManagement/clue',
  component: DEFAULT_LAYOUT,
  children: [
    {
      path: 'clue',
      name: ClueRouteEnum.CLUE_MANAGEMENT_CLUE,
      component: () => import('@/views/clueManagement/clue/index.vue'),
      meta: {
        locale: 'menu.clue',
        isTopMenu: true,
      },
    },
    {
      path: 'cluePool',
      name: ClueRouteEnum.CLUE_MANAGEMENT_POOL,
      component: () => import('@/views/clueManagement/cluePool/index.vue'),
      meta: {
        locale: 'module.cluePool',
        isTopMenu: true,
      },
    },
  ],
};

export default clue;
