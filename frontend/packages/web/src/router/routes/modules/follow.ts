import { FollowRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const follow: AppRouteRecordRaw = {
  path: '/follow',
  name: FollowRouteEnum.FOLLOW_MANAGEMENT,
  redirect: '/follow/record',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'module.followManagement',
    permissions: ['FOLLOW_RECORD:READ', 'FOLLOW_PLAN:READ'],
    icon: 'iconicon_account_outlined',
    hideChildrenInMenu: true,
    collapsedLocale: 'module.followManagement',
  },
  children: [
    {
      path: 'record',
      name: FollowRouteEnum.FOLLOW_MANAGEMENT_RECORD,
      component: () => import('@/views/follow/index.vue'),
      props: { type: 'followRecord' },
      meta: {
        locale: 'module.customer.followRecord',
        isTopMenu: true,
        permissions: ['FOLLOW_RECORD:READ'],
      },
    },
    {
      path: 'plan',
      name: FollowRouteEnum.FOLLOW_MANAGEMENT_PLAN,
      component: () => import('@/views/follow/index.vue'),
      props: { type: 'followPlan' },
      meta: {
        locale: 'module.customer.followPlan',
        isTopMenu: true,
        permissions: ['FOLLOW_PLAN:READ'],
      },
    },
  ],
};

export default follow;
