import { createRouter, createWebHistory } from 'vue-router';

import createRouteGuard from './guard/index';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: 'login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/base/login/index.vue'),
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/demo',
      name: 'demo',
      component: () => import('@/views/TableDemo.vue'),
    },
  ],
});

createRouteGuard(router);

export default router;
