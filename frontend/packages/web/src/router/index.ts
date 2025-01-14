import { createRouter, createWebHistory } from 'vue-router';

import createRouteGuard from './guard/index';
import appRoutes from './routes';
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
    ...appRoutes,
  ],
});

createRouteGuard(router);

export default router;
