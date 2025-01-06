import { createRouter, createWebHistory } from 'vue-router';

import createRouteGuard from './guard/index';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'index',
      component: () => import('../views/AboutView.vue'),
    },
  ],
});

createRouteGuard(router);

export default router;
