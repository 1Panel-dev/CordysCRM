import { AUTH_DISABLED_ROUTE_NAME, NO_RESOURCE_ROUTE_NAME } from '../constants';
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

export const AUTH_DISABLED_ROUTE: RouteRecordRaw = {
  path: '/authDisabled',
  name: AUTH_DISABLED_ROUTE_NAME,
  component: () => import('@/views/base/auth-disabled.vue'),
  meta: {
    requiresAuth: false,
  },
};
