import { CustomFormRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const customForm: AppRouteRecordRaw = {
  path: '/customForm',
  name: CustomFormRouteEnum.CUSTOM_FORM,
  redirect: '/customForm/index',
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.customForm',
    permissions: ['CUSTOM_FORM:READ'],
    icon: 'iconicon_form',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.customForm',
  },
  children: [
    {
      path: 'index',
      name: CustomFormRouteEnum.CUSTOM_FORM_INDEX,
      component: () => import('@/views/customForm/index.vue'),
      meta: {
        locale: 'menu.customForm',
        permissions: ['CUSTOM_FORM:READ'],
      },
    },
  ],
};

export default customForm;
