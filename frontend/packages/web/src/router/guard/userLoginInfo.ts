import { sleep } from '@lib/shared/method';
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

    const tokenExists = hasToken(to.name as string);

    // 如果目标页是 login 且已经登录，则重定向到有权限的第一个路由
    if (to.name === 'login' && tokenExists) {
      const currentRouteName = getFirstRouteNameByPermission(router.getRoutes());
      next({ name: currentRouteName });
      NProgress.done();
      return;
    }

    // 有 token，直接放行
    if (to.name !== 'login' && tokenExists) {
      next();
      return;
    }

    // 去登录页 or 白名单页，（比如公开页面）放行
    if (to.name === 'login' || isWhiteListPage()) {
      next();
      return;
    }

    // 没 token 且访问受限页面 跳转登录页并带上 redirect
    next({
      name: 'login',
      query: {
        redirect: to.name,
        ...to.query,
      } as LocationQueryRaw,
    });

    await sleep(0);
    NProgress.done();
  });
}
