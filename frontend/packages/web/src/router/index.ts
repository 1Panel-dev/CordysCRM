import { createRouter, createWebHistory } from 'vue-router';

import { DEFAULT_LAYOUT } from './base';
import createRouteGuard from './guard/index';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'index',
      component: DEFAULT_LAYOUT,
    },
  ],
});

createRouteGuard(router);

export default router;
