import { createRouter, createWebHashHistory } from 'vue-router';

import 'nprogress/nprogress.css';
import createRouteGuard from './guard/index';
import appRoutes from './routes';
import { NO_RESOURCE_ROUTE, NOT_FOUND_ROUTE } from './routes/base';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHashHistory(),
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
    NOT_FOUND_ROUTE,
    NO_RESOURCE_ROUTE,
  ],
});

createRouteGuard(router);

export default router;
