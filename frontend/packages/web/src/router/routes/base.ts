import { NO_RESOURCE_ROUTE_NAME } from '../constants';
import type { RouteRecordRaw } from 'vue-router';

export const DEFAULT_LAYOUT = () => import('@/layout/default-layout.vue');

export const NOT_FOUND_ROUTE: RouteRecordRaw = {
  path: '/:pathMatch(.*)*',
  name: 'notFound',
  component: () => import('@/views/base/not-found/index.vue'),
  meta: {
    requiresAuth: false,
  },
};

export const NO_RESOURCE_ROUTE: RouteRecordRaw = {
  path: '/noResource',
  name: NO_RESOURCE_ROUTE_NAME,
  component: () => import('@/views/base/no-resource/index.vue'),
  meta: {
    requiresAuth: false,
  },
};
