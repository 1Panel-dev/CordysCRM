import { clearToken, hasToken, isLoginExpires } from '@lib/shared/method/auth';

import useUser from '@/hooks/useUser';

import { AppRouteEnum } from '@/enums/routeEnum';

import NProgress from 'nprogress';
import type { Router } from 'vue-router';

export default function setupUserLoginInfoGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    NProgress.start();

    const { isWhiteListPage } = useUser();
    // 登录过期清除token
    if (isLoginExpires()) {
      clearToken();
    }

    const tokenExists = hasToken(to.name as string);

    // 未登录访问受限页面重定向登录页
    if (!tokenExists && to.name !== 'login' && !isWhiteListPage()) {
      next({
        name: 'login',
      });
      NProgress.done();
      return;
    }

    // 已登录访问 login重定向（有权限第一个页面）
    if (to.name === 'login' && tokenExists) {
      next({ name: AppRouteEnum.WORKBENCH });
      NProgress.done();
      return;
    }

    // 其他情况（放行：已登录访问正常页面\未登录访问白名单页面）
    next();
    NProgress.done();
  });
}
