import { createRouter, createWebHashHistory } from 'vue-router';

import 'nprogress/nprogress.css';
import createRouteGuard from './guard/index';
import appRoutes from './routes';
import { AUTH_DISABLED_ROUTE, AUTH_LOGIN_LOADING_ROUTE, NO_RESOURCE_ROUTE } from './routes/base';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/loading',
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
    NO_RESOURCE_ROUTE,
    AUTH_DISABLED_ROUTE,
    AUTH_LOGIN_LOADING_ROUTE,
  ],
});

createRouteGuard(router);

export default router;
