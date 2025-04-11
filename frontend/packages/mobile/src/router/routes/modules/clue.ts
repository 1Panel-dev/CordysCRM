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
      path: 'detail',
      name: ClueRouteEnum.CLUE_DETAIL,
      component: () => import('@/views/clue/clue/detail.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'poolDetail',
      name: ClueRouteEnum.CLUE_POOL_DETAIL,
      component: () => import('@/views/clue/pool/detail.vue'),
      meta: {
        permissions: [],
      },
    },
  ],
};

export default clue;
