import { createRouter, createWebHashHistory } from 'vue-router';

import { WorkbenchRouteEnum } from '@/enums/routeEnum';

import 'nprogress/nprogress.css';
import createRouteGuard from './guard/index';
import appRoutes from './routes';
import { AUTH_DISABLED_ROUTE, NO_RESOURCE_ROUTE } from './routes/base';
import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: WorkbenchRouteEnum.WORKBENCH_INDEX,
    },
    ...appRoutes,
    NO_RESOURCE_ROUTE,
    AUTH_DISABLED_ROUTE,
  ],
});

createRouteGuard(router);

export default router;
