import { MineRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const mine: AppRouteRecordRaw = {
  path: '/mine',
  name: MineRouteEnum.MINE,
  redirect: '/mine/index',
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'index',
      name: MineRouteEnum.MINE_INDEX,
      component: () => import('@/views/mine/index.vue'),
      meta: {
        locale: 'menu.mine',
        permissions: [],
      },
    },
    {
      path: 'message',
      name: MineRouteEnum.MINE_MESSAGE,
      component: () => import('@/views/mine/message.vue'),
      meta: {
        locale: 'menu.message',
        permissions: [],
      },
    },
    {
      path: 'detail',
      name: MineRouteEnum.MINE_DETAIL,
      component: () => import('@/views/mine/detail.vue'),
      meta: {
        permissions: [],
      },
    },
  ],
};

export default mine;
