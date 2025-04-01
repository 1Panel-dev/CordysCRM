import { NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '../constants';
import usePermission from '@cordys/web/src/hooks/usePermission';
import NProgress from 'nprogress';
import type { Router } from 'vue-router';

export default function setupPermissionGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const Permission = usePermission();
    const permissionsAllow = Permission.accessRouter(to);

    // TODO 如果是隐藏的模块，则跳转到无权限页面

    const exist = WHITE_LIST.find((el) => el.name === to.name);
    if (exist || permissionsAllow) {
      next();
    } else {
      next({
        name: NO_RESOURCE_ROUTE_NAME,
      });
    }
    NProgress.done();
  });
}
