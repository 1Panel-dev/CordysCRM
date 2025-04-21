import { NO_RESOURCE_ROUTE_NAME } from '../constants';
import type { RouteRecordRaw } from 'vue-router';

export const DEFAULT_LAYOUT = () => import('@/layout/default-layout.vue');

export const NO_RESOURCE_ROUTE: RouteRecordRaw = {
  path: '/noResource',
  name: NO_RESOURCE_ROUTE_NAME,
  component: () => import('@/views/base/no-resource.vue'),
  meta: {
    requiresAuth: false,
  },
};
