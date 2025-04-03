import { CommonRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const common: AppRouteRecordRaw = {
  path: '/common',
  name: CommonRouteEnum.COMMON,
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [],
  },
  children: [
    {
      path: 'formCreate',
      name: CommonRouteEnum.FORM_CREATE,
      component: () => import('@/components/business/crm-form-create/index.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'contactDetail',
      name: CommonRouteEnum.CONTACT_DETAIL,
      component: () => import('@/components/business/crm-contact-list/detail.vue'),
      meta: {
        permissions: [],
      },
    },
    {
      path: 'followDetail',
      name: CommonRouteEnum.FOLLOW_DETAIL,
      component: () => import('@/components/business/crm-follow-list/followDetail.vue'),
      meta: {
        permissions: [],
      },
    },
  ],
};

export default common;
