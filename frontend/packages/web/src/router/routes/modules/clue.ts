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
      // TODO 更换页面
      component: () => import('@/views/opportunity/index.vue'),
      meta: {
        locale: 'menu.clue',
        isTopMenu: true,
      },
    },
    {
      path: 'cluePool',
      name: ClueRouteEnum.CLUE_MANAGEMENT_POOL,
      // TODO 更换页面
      component: () => import('@/views/system/module/index.vue'),
      meta: {
        locale: 'menu.cluePool',
        isTopMenu: true,
      },
    },
  ],
};

export default clue;
