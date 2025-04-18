import type { RouteRecordRaw } from 'vue-router';

export const DEFAULT_LAYOUT = () => import('@/layout/default-layout.vue');

export const LOGIN: RouteRecordRaw = {
  path: '/login',
  name: 'login',
  component: () => import('@/views/login.vue'),
  meta: {
    permissions: [],
  },
};
