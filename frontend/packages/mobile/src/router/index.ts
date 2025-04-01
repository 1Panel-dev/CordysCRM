import { createRouter, createWebHashHistory } from 'vue-router';

import createRouteGuard from './guard/index';
import appRoutes from './routes';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: 'workbench',
    },
    ...appRoutes,
  ],
});

createRouteGuard(router);

export default router;
