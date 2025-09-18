import { clearToken, hasToken, isLoginExpires } from '@lib/shared/method/auth';

import useUser from '@/hooks/useUser';
import { getFirstRouteNameByPermission } from '@/utils/permission';

import NProgress from 'nprogress';
import type { LocationQueryRaw, Router } from 'vue-router';

export default function setupUserLoginInfoGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    NProgress.start();

    const { isWhiteListPage } = useUser();
    // 登录过期清除token
    if (isLoginExpires()) {
      clearToken();
    }

    const tokenExists = hasToken();

    // 未登录访问受限页面重定向登录页
    if (!tokenExists && to.name !== 'login' && !isWhiteListPage()) {
      next({
        name: 'login',
        query: {
          redirect: to.name,
          ...to.query,
        } as LocationQueryRaw,
      });
      NProgress.done();
      return;
    }

    // 已登录访问 login重定向（有权限第一个页面）
    if (to.name === 'login' && tokenExists) {
      const firstRoute = getFirstRouteNameByPermission(router.getRoutes());
      next({ name: firstRoute });
      NProgress.done();
      return;
    }

    // 其他情况（放行：已登录访问正常页面\未登录访问白名单页面）
    next();
    NProgress.done();
  });
}
