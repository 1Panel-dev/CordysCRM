import { ClueRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const clue: AppRouteRecordRaw = {
  path: '/clue',
  name: ClueRouteEnum.CLUE,
  redirect: '/clue/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'index',
      name: ClueRouteEnum.CLUE_INDEX,
      component: () => import('@/views/clue/index.vue'),
      meta: {
        locale: 'menu.clue',
        permissions: [],
      },
    },
    {
      path: 'pool',
      name: ClueRouteEnum.CLUE_POOL,
      component: () => import('@/views/clue/pool.vue'),
      meta: {
        locale: 'menu.cluePool',
        permissions: [],
      },
    },
  ],
};

export default clue;
