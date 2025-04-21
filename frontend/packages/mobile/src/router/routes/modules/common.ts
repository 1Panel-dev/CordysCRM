import { CommonRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const common: AppRouteRecordRaw = {
  path: '/common',
  name: CommonRouteEnum.COMMON,
  component: DEFAULT_LAYOUT,
  meta: {
    permissions: [
      'CLUE_MANAGEMENT:ADD',
      'CLUE_MANAGEMENT:UPDATE',
      'CUSTOMER_MANAGEMENT:ADD',
      'CUSTOMER_MANAGEMENT:UPDATE',
      'CUSTOMER_MANAGEMENT_CONTACT:ADD',
      'CUSTOMER_MANAGEMENT_CONTACT:UPDATE',
      'OPPORTUNITY_MANAGEMENT:ADD',
      'OPPORTUNITY_MANAGEMENT:UPDATE',
    ],
  },
  children: [
    {
      path: 'formCreate',
      name: CommonRouteEnum.FORM_CREATE,
      component: () => import('@/components/business/crm-form-create/index.vue'),
      meta: {
        permissions: [
          'CLUE_MANAGEMENT:ADD',
          'CLUE_MANAGEMENT:UPDATE',
          'CUSTOMER_MANAGEMENT:ADD',
          'CUSTOMER_MANAGEMENT:UPDATE',
          'CUSTOMER_MANAGEMENT_CONTACT:ADD',
          'CUSTOMER_MANAGEMENT_CONTACT:UPDATE',
          'OPPORTUNITY_MANAGEMENT:ADD',
          'OPPORTUNITY_MANAGEMENT:UPDATE',
        ],
        depth: 9,
      },
    },
    {
      path: 'contactDetail',
      name: CommonRouteEnum.CONTACT_DETAIL,
      component: () => import('@/components/business/crm-contact-list/detail.vue'),
      meta: {
        permissions: ['CUSTOMER_MANAGEMENT:READ'],
        depth: 9,
      },
    },
    {
      path: 'followDetail',
      name: CommonRouteEnum.FOLLOW_DETAIL,
      component: () => import('@/components/business/crm-follow-list/followDetail.vue'),
      meta: {
        permissions: ['CUSTOMER_MANAGEMENT:READ', 'CLUE_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:READ'],
        depth: 9,
      },
    },
  ],
};

export default common;
