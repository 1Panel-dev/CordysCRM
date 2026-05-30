import { NO_RESOURCE_ROUTE_NAME, NO_RESOURCE_ROUTE_NAME_INDEX } from '../constants';
import type { RouteRecordRaw } from 'vue-router';

export const DEFAULT_LAYOUT = () => import('@/layout/default-layout.vue');
export const FULL_PAGE_LAYOUT = () => import('@/layout/full-page-layout.vue');

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
  redirect: '/noResource/index',
  component: DEFAULT_LAYOUT,
  meta: {
    requiresAuth: false,
  },
  children: [
    {
      path: 'index',
      name: NO_RESOURCE_ROUTE_NAME_INDEX,
      component: () => import('@/views/base/no-resource/index.vue'),
    },
  ],
};
